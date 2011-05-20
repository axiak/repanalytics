package service.twitter;


import com.google.common.base.Predicate;
import models.businesses.Business;
import models.businesses.Review;
import org.apache.commons.lang.math.RandomUtils;
import play.Logger;
import play.Play;
import play.cache.Cache;
import service.ReviewSource;
import service.reviews.ReviewFetcher;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static play.libs.Codec.decodeBASE64;

public class TwitterService implements ReviewFetcher {
    private boolean isStreaming = false;
    private int maxReviews = -1;

    private static Configuration getTwitterConfiguration() {
        Properties conf = Play.configuration;
        return new ConfigurationBuilder()
                .setOAuthAccessToken(conf.getProperty("twitter.oauth.accessToken"))
                .setOAuthAccessTokenSecret(conf.getProperty("twitter.oauth.accessTokenSecret"))
                .setOAuthConsumerKey(conf.getProperty("twitter.oauth.consumerKey"))
                .setOAuthConsumerSecret(conf.getProperty("twitter.oauth.consumerSecret"))
                .setUser(conf.getProperty("twitter.user"))
                .setPassword(new String(decodeBASE64(conf.getProperty("twitter.password.base64"))))
                .setPrettyDebugEnabled(true)
                .setDebugEnabled(true)
                .build();
    }

    public TwitterService setMaxReviews(int maxReviews) {
        this.maxReviews = maxReviews;
        return this;
    }

    public TwitterService setIsStreaming(boolean streaming) {
        this.isStreaming = streaming;
        return this;
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        if (isStreaming) {
            return getReviewsStreaming(business);
        } else {
            String cacheKey = "twitter_reviews_" + business.id + "_" + maxReviews;
            Logger.info("Cache key: %s", cacheKey);
            @SuppressWarnings("unchecked")
            List<Review> reviews = Cache.get(cacheKey, List.class);
            if (reviews == null) {
                reviews = getReviewsActual(business);
                Cache.set(cacheKey, reviews);
            }
            return reviews;
        }
    }

    private List<Review> getReviewsStreaming(@Nonnull Business business) {
        TwitterStream twitterStream = new TwitterStreamFactory(getTwitterConfiguration()).getInstance();
        List<Review> reviews = new ArrayList<Review>();
        BusinessStatusListener businessStatusListener = new BusinessStatusListener();
        twitterStream.addListener(businessStatusListener);
        twitterStream.filter(new FilterQuery().track(new String[]{business.name}));
        try {
            businessStatusListener.await(5, TimeUnit.MINUTES);
        } finally {
            twitterStream.shutdown();
        }
        List<Status> statuses = businessStatusListener.getStatuses();
        for (Status status : statuses) {
            Review review = new Review();
            review.text = status.getText();
            review.source = ReviewSource.TWITTER;
            review.userName = status.getUser().getName();
            review.sourceUrl = "http://twitter.com/intent/user?screen_name=" + review.userName;
            review.business = business;
            review.date = status.getCreatedAt();
            reviews.add(review);
        }
        return reviews;
    }

    private List<Review> getReviewsActual(@Nonnull Business business) {
        if (business.name == null) {
            return null;
        }
        boolean lookupDistance = business.latitude != null && business.longitude != null;

        List<Review> reviews = new ArrayList<Review>();
        Twitter twitter = new TwitterFactory(getTwitterConfiguration()).getInstance();
        try {
            Query query = new Query(business.name + randomSpaces());
            QueryResult result = twitter.search(query);
            for (Tweet tweet : result.getTweets()) {
                GeoLocation loc = tweet.getGeoLocation();
                Review review = new Review();
                review.text = tweet.getText();
                review.source = ReviewSource.TWITTER;
                review.business = business;
                review.userName = tweet.getFromUser();
                review.sourceUrl = "http://twitter.com/intent/user?screen_name=" + review.userName;
                review.date = tweet.getCreatedAt();
                reviews.add(review);
            }
        } catch (TwitterException e) {
            Logger.error("Twitter failed!: %s", e);
            Logger.error(e, "Could not search twitter.");
        }
        Logger.info("MaxReviews: %s", maxReviews);
        if (maxReviews > 0 && reviews.size() > maxReviews) {
            Collections.shuffle(reviews);
            reviews = new ArrayList<Review>(reviews.subList(0, maxReviews));
        }
        return reviews;
    }

    public class IsDateRecent implements Predicate<Review> {
        private Date lastMessageDate;

        private IsDateRecent(Date lastMessageDate) {
            this.lastMessageDate = lastMessageDate;
        }

        @Override
        public boolean apply(@Nullable Review review) {
            return review != null && review.date != null && lastMessageDate.compareTo(review.date) < 0;
        }
    }

    public class BusinessStatusListener implements StatusListener {
        private CountDownLatch latch;
        private List<Status> statuses;

        public BusinessStatusListener() {
            latch = new CountDownLatch(1);
            statuses = new ArrayList<Status>();
        }

        public List<Status> getStatuses() {
            return statuses;
        }

        public void await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void await(long timeout, TimeUnit unit) {
            try {
                latch.await(timeout, unit);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStatus(Status status) {
            statuses.add(status);
            latch.countDown();
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
            latch.countDown();
            latch.countDown();
            e.printStackTrace();
        }
    }

    public class TwitterStatuses extends RuntimeException {
        private List<Status> statuses;
        public TwitterStatuses(List<Status> statuses) {
            this.statuses = statuses;
        }

        public List<Status> getStatuses() {
            return this.statuses;
        }
    }

    private String randomSpaces() {
        return com.google.common.base.Strings.repeat(" ", RandomUtils.nextInt(250));
    }
}
