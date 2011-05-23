package service.twitter;

import models.businesses.Business;
import models.businesses.Review;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.util.LRUMap;
import play.Play;
import play.libs.F;
import service.ReviewSource;
import service.reviews.RealtimeReviewFetcher;
import twitter4j.*;

import javax.annotation.Nonnull;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static service.twitter.TwitterService.getTwitterConfiguration;
import static util.NaturalLanguages.reviewSentiment;
import static util.Strings.normalizeSimple;

public final class TwitterRealtimeFetcher implements RealtimeReviewFetcher, StatusListener {
    private Log logger = LogFactory.getLog(getClass());
    private AtomicReference<TwitterStream> stream = new AtomicReference<TwitterStream>(null);
    private transient long exceptionWaitTime = 4L;
    private static final long maxExceptionWaitTime = 2048;
    private final Map<String, BusinessListener> listenerMap;
    private transient String[] filterKeywords = new String[]{};

    private static AtomicReference<TwitterRealtimeFetcher> instance = new AtomicReference<TwitterRealtimeFetcher>(null);

    @SuppressWarnings("unchecked")
    private TwitterRealtimeFetcher() {
        int maxKeywords = Integer.valueOf(Play.configuration.getProperty("twitter.max.simultaneous.keywords", "400"));
        listenerMap = Collections.synchronizedMap(new LRUMap(maxKeywords));
    }

    public static TwitterRealtimeFetcher getInstance() {
        TwitterRealtimeFetcher newInstance = new TwitterRealtimeFetcher();
        if (!instance.compareAndSet(null, newInstance)) {
            newInstance = instance.get();
        }
        return newInstance;
    }

    @Override
    public F.Promise<List<Review>> startBusiness(@Nonnull Business business) {
        String name = normalizeSimple(business.name);
        BusinessListener listener = new BusinessListener(business);
        synchronized (listenerMap) {
            if (!listenerMap.containsKey(name)) {
                listenerMap.put(name, listener);
                reconnectTwitterStream();
            } else {
                listener = listenerMap.get(name);
            }
        }
        return listener.getPromise();
    }

    @Override
    public F.Promise<List<Review>> getReviewsOnReady(@Nonnull Business business) {
        String name = normalizeSimple(business.name);
        try {
            return listenerMap.get(name).getPromise();
        } catch (NullPointerException e) {
            return startBusiness(business);
        }
    }

    @Override
    public void cancelPromise(@Nonnull Business business) {
        String name = normalizeSimple(business.name);
        BusinessListener listener = listenerMap.get(name);
        // todo - maybe more ref counting logic here?
    }

    @Override
    public void shutdown(@Nonnull Business business) {
        String name = normalizeSimple(business.name);
        if (listenerMap.remove(name) != null) {
            reconnectTwitterStream();
        }
    }

    private synchronized void shutdownAll() {
        TwitterStream myStream = stream.getAndSet(null);
        myStream.shutdown();
        for (BusinessListener listener : listenerMap.values()) {
            listener.forceEvaluation();
        }
        listenerMap.clear();
    }

    @Override
    public void cleanOldBusinesses(String timeout) {
        Set<String> startStrings = listenerMap.keySet();
        boolean changedKeywords = false;
        long timeoutMillis = play.libs.Time.parseDuration(timeout) * 1000L;
        long earliestTime = new Date().getTime() - timeoutMillis;
        for (Map.Entry<String, BusinessListener> entry : listenerMap.entrySet()) {
            if (entry.getValue().getLastTouchTime() < earliestTime) {
                listenerMap.remove(entry.getKey());
                logger.info(String.format("Removing twitter listener: '%s'", entry.getKey()));
                changedKeywords = true;
            }
        }

        if (changedKeywords && !listenerMap.keySet().equals(startStrings)) {
            reconnectTwitterStream();
        }
    }

    @Override
    public void onStatus(Status status) {
        String normalizedText = normalizeSimple(status.getText());
        for (BusinessListener listener : listenerMap.values()) {
            if (listener.handleStatus(status, normalizedText)) {
                break;
            }
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
        logger.error("Failure in twitter stream.", e);
        if (exceptionWaitTime < maxExceptionWaitTime) {
            try {
                logger.info(String.format("Waiting %s seconds in twitter thread.", exceptionWaitTime));
                Thread.sleep(1000L * exceptionWaitTime);
            } catch (InterruptedException e1) {
                logger.info("Somehow we were interrupted while waiting.", e1);
            }
            exceptionWaitTime <<= 1;
        } else {
            logger.error("Unrecoverable twitter failure.", e);
            shutdownAll();
        }
    }

    private void reconnectTwitterStream() {
        Set<String> businessNames = listenerMap.keySet();
        logger.info(String.format("Reconnecting to twitter stream for keywords: %s", businessNames));
        filterKeywords = businessNames.toArray(new String[businessNames.size()]);
        TwitterStream newStream = new TwitterStreamFactory(getTwitterConfiguration()).getInstance();
        newStream.addListener(this);
        TwitterStream oldStream = stream.getAndSet(newStream);
        if (oldStream != null) {
            oldStream.shutdown();
        }
        newStream.filter(new FilterQuery().track(filterKeywords));
    }


    private final static class BusinessListener {
        private String name;
        private Business business;
        private volatile long lastTouchTime;
        private AtomicReference<F.Promise<List<Review>>> promise;
        private AtomicReference<Collection<Review>> reviewBuffer;

        @SuppressWarnings("unchecked")
        public BusinessListener(Business business) {
            this.name = normalizeSimple(business.name);
            this.business = business;
            this.promise = new AtomicReference<F.Promise<List<Review>>>(null);
            this.reviewBuffer = new AtomicReference<Collection<Review>>(new CircularFifoBuffer(8));
            this.lastTouchTime = new Date().getTime();
        }

        public boolean matchesStatus(String statusText) {
            return statusText.contains(name);
        }

        public boolean handleStatus(Status status, String normalizedText) {
            if (matchesStatus(normalizedText)) {
                Review review = statusToReview(status);
                addReview(review);
                if (this.promise.get() != null) {
                    F.Promise<List<Review>> newPromise = new F.Promise<List<Review>>();
                    F.Promise<List<Review>> oldPromise = this.promise.getAndSet(newPromise);
                    List<Review> reviews = getAndResetBuffer();
                    oldPromise.invoke(reviews);
                }
                return true;
            } else {
                return false;
            }
        }

        public Business getBusiness() {
            return business;
        }

        public long getLastTouchTime() {
            return lastTouchTime;
        }

        public F.Promise<List<Review>> getPromise() {
            F.Promise<List<Review>> myPromise = new F.Promise<List<Review>>();
            if (!promise.compareAndSet(null, myPromise)) {
                myPromise = promise.get();
            }
            lastTouchTime = new Date().getTime();
            return myPromise;
        }

        @SuppressWarnings("unchecked")
        public List<Review> getAndResetBuffer() {
            lastTouchTime = new Date().getTime();
            Collection<Review> reviews = reviewBuffer.getAndSet(new CircularFifoBuffer(8));
            return new ArrayList<Review>(reviews);
        }

        public void addReview(Review review) {
            reviewBuffer.get().add(review);
        }

        private Review statusToReview(Status status) {
            Review review = new Review();
            review.text = status.getText();
            review.source = ReviewSource.TWITTER;
            review.userName = status.getUser().getName();
            review.sourceUrl = "http://twitter.com/intent/user?screen_name=" + review.userName;
            review.date = status.getCreatedAt();
            review.business = business;
            reviewSentiment(review);
            return review;
        }

        private void forceEvaluation() {
            F.Promise<List<Review>> oldPromise = promise.getAndSet(null);
            if (oldPromise != null) {
                oldPromise.invoke(getAndResetBuffer());
            }
        }
    }
}