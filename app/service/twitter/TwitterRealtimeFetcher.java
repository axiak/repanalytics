package service.twitter;

import models.businesses.Business;
import models.businesses.Review;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import play.Logger;
import play.libs.F;
import service.ReviewSource;
import service.reviews.RealtimeReviewFetcher;
import sun.reflect.generics.tree.VoidDescriptor;
import twitter4j.*;

import javax.annotation.Nonnull;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static play.libs.Time.*;
import static service.twitter.TwitterService.getTwitterConfiguration;
import static util.NaturalLanguages.reviewSentiment;

public class TwitterRealtimeFetcher implements RealtimeReviewFetcher {
    private Map<String, BusinessStatusListener> listeners;
    private ConcurrentMap<String, AtomicInteger> listenerRefCounters;
    private ConcurrentMap<String, AtomicInteger> promiseRefCounters;

    public TwitterRealtimeFetcher() {
        listeners = new ConcurrentHashMap<String, BusinessStatusListener>();
        listenerRefCounters = new ConcurrentHashMap<String, AtomicInteger>();
        promiseRefCounters = new ConcurrentHashMap<String, AtomicInteger>();
    }

    public void cleanOldBusinesses(String timeout) {
        long millisAge = parseDuration(timeout) * 1000;
        for (Map.Entry<String, BusinessStatusListener> entry : listeners.entrySet()) {
            if (entry.getValue().getAgeInMillis() > millisAge) {
                Logger.info("Removing twitter stream since it's %s seconds old: '%s'",
                            millisAge / 1000, entry.getKey());
                entry.getValue().getTwitterStream().shutdown();
                listeners.remove(entry.getKey());
                listenerRefCounters.remove(entry.getKey());
                promiseRefCounters.remove(entry.getKey());
            }
        }
    }

    @Override
    public void startBusiness(@Nonnull Business business) {
        AtomicInteger value = new AtomicInteger(1);
        value = listenerRefCounters.putIfAbsent(business.name, value);
        if (value == null || value.getAndIncrement() == 0) {
            startBusinessWithReturn(business);
        }
    }

    private BusinessStatusListener startBusinessWithReturn(@Nonnull Business business) {
        TwitterStream twitterStream = new TwitterStreamFactory(getTwitterConfiguration()).getInstance();
        BusinessStatusListener businessStatusListener = new BusinessStatusListener(twitterStream, business, this);
        twitterStream.addListener(businessStatusListener);
        twitterStream.filter(new FilterQuery().track(new String[]{business.name}));
        listeners.put(business.name, businessStatusListener);
        return businessStatusListener;
    }

    @Override
    public F.Promise<List<Review>> getReviewsOnReady(@Nonnull Business business) {
        BusinessStatusListener listener = listeners.get(business.name);
        if (listener == null) {
            listener = startBusinessWithReturn(business);
        }
        AtomicInteger value = new AtomicInteger(1);
        value = promiseRefCounters.putIfAbsent(business.name, value);
        if (value != null) {
            value.incrementAndGet();
        }
        return listener.getReviewPromise();
    }

    @Override
    public void cancelPromise(@Nonnull Business business) {
        if (promiseRefCounters.get(business.name).decrementAndGet() == 0) {
            promiseRefCounters.remove(business.name);
            listeners.get(business.name).cancelPromise();
        }
    }

    @Override
    public void shutdown(@Nonnull Business business) {
        if (listenerRefCounters.get(business.name).decrementAndGet() == 0) {
            BusinessStatusListener listener = listeners.remove(business.name);
            if (listener != null) {
                listener.getTwitterStream().shutdown();
            }
            listenerRefCounters.remove(business.name);
        }
    }

    private static class BusinessStatusListener implements StatusListener {
        /* Buffer will not hold more than 8 elements */
        private AtomicReference<CircularFifoBuffer> buffer = new AtomicReference<CircularFifoBuffer>();
        private AtomicReference<F.Promise<List<Review>>> promise = new AtomicReference<F.Promise<List<Review>>>();
        private long lastTouchTime = 0;
        private TwitterRealtimeFetcher fetcher;
        private TwitterStream twitterStream = null;
        private long exceptionWait = 4;
        private static final int BUFFER_SIZE = 8;
        private Business business;

        public BusinessStatusListener(TwitterStream twitterStream, Business business, TwitterRealtimeFetcher fetcher) {
            this.twitterStream = twitterStream;
            this.business = business;
            this.fetcher = fetcher;
            getAndResetBuffer();
        }

        private CircularFifoBuffer getAndResetBuffer() {
            return buffer.getAndSet(new CircularFifoBuffer(BUFFER_SIZE));
        }

        private Review statusToReview(Status status) {
            Review review = new Review();
            review.text = status.getText();
            review.source = ReviewSource.TWITTER;
            review.userName = status.getUser().getName();
            review.sourceUrl = "http://twitter.com/intent/user?screen_name=" + review.userName;
            review.business = business;
            review.date = status.getCreatedAt();
            reviewSentiment(review);
            return review;
        }

        public TwitterStream getTwitterStream() {
            return twitterStream;
        }

        public F.Promise<List<Review>> getReviewPromise() {
            F.Promise<List<Review>> newPromise = new F.Promise<List<Review>>();
            if (!promise.compareAndSet(null, newPromise)) {
                newPromise = promise.get();
            }
            lastTouchTime = new Date().getTime();
            return newPromise;
        }

        public void cancelPromise() {
            promise.set(null);
        }

        public long getAgeInMillis() {
            return new Date().getTime() - lastTouchTime;
        }

        @Override
        public void onStatus(Status status) {
            F.Promise<List<Review>> actualPromise = promise.getAndSet(null);
            Review review = statusToReview(status);
            CircularFifoBuffer currentBuffer = actualPromise == null ? buffer.get() : getAndResetBuffer();
            synchronized (currentBuffer) {
                currentBuffer.add(review);
            }
            if (actualPromise != null) {
                @SuppressWarnings("unchecked")
                List<Review> response = new ArrayList<Review>(currentBuffer);
                actualPromise.invoke(response);
                lastTouchTime = new Date().getTime();
            }
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
        }

        @Override
        public void onTrackLimitationNotice(int i) {
        }

        @Override
        public void onScrubGeo(long l, long l1) {
        }

        @Override
        public void onException(Exception e) {
            Logger.error(e, "Exception with twitter stream");
            Logger.error("Error with twitter stream: %s", e);
            if (exceptionWait < 2048) {
                try {
                    Logger.info("Waiting %s seconds in twitter stream.", exceptionWait);
                    Thread.sleep(1000L * exceptionWait);
                } catch (InterruptedException e1) {
                    Logger.info("Somehow our thread got interrupted: %s", e);
                }
                exceptionWait <<= 1;
            } else {
                Logger.error("Unrecoverable twitter failure.");
                fetcher.shutdown(business);
            }
        }
    }
}
