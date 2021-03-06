package service.twitter;


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
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static play.libs.Codec.decodeBASE64;

public class TwitterService implements ReviewFetcher {
    private int maxReviews = -1;
    private long minDate = -1;

    public static Configuration getTwitterConfiguration() {
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

    public TwitterService setReviewMinDate(long minDate) {
        this.minDate = minDate;
        return this;
    }

    public TwitterService setMaxReviews(int maxReviews) {
        this.maxReviews = maxReviews;
        return this;
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        if (minDate > 0) {
            return getReviewsMoreRecentThanDate(new Date(this.minDate), business);
        } else {
            String cacheKey = "twitter_reviews_" + business.id + "_" + maxReviews;
            Logger.info("Cache key: %s", cacheKey);
            @SuppressWarnings("unchecked")
            List<Review> reviews = Cache.get(cacheKey, List.class);
            if (reviews == null) {
                reviews = getReviewsActual(business);
            }
            Cache.set(cacheKey, reviews, "1min");
            return reviews;
        }
    }

    private List<Review> getReviewsMoreRecentThanDate(Date minDate, @Nonnull Business business) {
        List<Review> reviews = new ArrayList<Review>();

        int numTries = 0;
        while ((numTries++ < 30) && (reviews = new ArrayList<Review>(filter(getReviewsActual(business), new IsDateRecent(minDate)))).size() == 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return reviews;
    }

    private List<Review> getReviewsActual(@Nonnull Business business) {
        if (business.name == null) {
            return null;
        }
        List<Review> reviews = new ArrayList<Review>();
        Twitter twitter = new TwitterFactory(getTwitterConfiguration()).getInstance();
        try {
            Query query = new Query(business.name + randomSpaces());
            query.setLang("en");
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
        if (maxReviews > 0 && reviews.size() > maxReviews) {
            Collections.shuffle(reviews);
            reviews = new ArrayList<Review>(reviews.subList(0, maxReviews));
        }
        return reviews;
    }

    private String randomSpaces() {
        return com.google.common.base.Strings.repeat(" ", RandomUtils.nextInt(250));
    }
}
