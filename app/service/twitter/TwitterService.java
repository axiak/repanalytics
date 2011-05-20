package service.twitter;


import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
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
import util.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static play.libs.Codec.decodeBASE64;

public class TwitterService implements ReviewFetcher {
    @Nullable private Date lastMessageDate;
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

    public TwitterService setLastMessageDate(@Nullable Date messageDate) {
        lastMessageDate = messageDate;
        return this;
    }

    public TwitterService setMaxReviews(int maxReviews) {
        this.maxReviews = maxReviews;
        return this;
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        if (lastMessageDate != null) {
            return getReviewsFiltered(lastMessageDate, business);
        } else {
            String cacheKey = "twitter_reviews_" + business.id;
            @SuppressWarnings("unchecked")
            List<Review> reviews = Cache.get(cacheKey, List.class);
            if (reviews == null) {
                reviews = getReviewsActual(business);
                Cache.set(cacheKey, reviews);
            }
            return reviews;
        }
    }

    private List<Review> getReviewsFiltered(@Nonnull final Date lastMessageDate, @Nonnull Business business) {
        TwitterStream twitterStream = new TwitterStreamFactory(getTwitterConfiguration()).getInstance();
        List<Review> reviews = new ArrayList<Review>();
        try {
            twitterStream.addListener(new BusinessStatusListener());
            twitterStream.filter(new FilterQuery().track(new String[]{business.name}));
        } catch (TwitterStatuses statusException) {
            List<Status> statuses = statusException.getStatuses();
            for (Status status : statuses) {
                Review review = new Review();
                review.text = status.getText();
                review.source = ReviewSource.TWITTER;
                review.userName = status.getUser().getName();
                review.sourceUrl = "http://www.twitter.com/" + review.userName;
                review.business = business;
                review.date = status.getCreatedAt();
                reviews.add(review);
            }
        }

        Logger.info("Left status filterrer...sleeping");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        twitterStream.shutdown();
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
            if (result.getTweets().size() > 0) {
                Logger.info("Date: %s", result.getTweets().get(0).getCreatedAt());
            }
            for (Tweet tweet : result.getTweets()) {
                GeoLocation loc = tweet.getGeoLocation();
                if (lookupDistance && loc != null) {
                    Logger.info("Tweet info: %s,%s", loc.getLatitude(), loc.getLongitude());
                }
                Review review = new Review();
                review.text = tweet.getText();
                review.source = ReviewSource.TWITTER;
                review.business = business;
                review.date = tweet.getCreatedAt();
                reviews.add(review);
            }
        } catch (TwitterException e) {
            Logger.error("Twitter failed!: %s", e);
            Logger.error(e, "Could not search twitter.");
        }

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
        @Override
        public void onStatus(Status status) {
            Logger.error("Received status!!");
            //throw new TwitterStatuses(Arrays.asList(status));
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
            //Logger.error(e,  "Error with twitter streaming api.");
            //Logger.error("Error: %s", e);
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
