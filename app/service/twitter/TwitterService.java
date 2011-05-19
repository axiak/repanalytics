package service.twitter;


import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import models.businesses.Business;
import models.businesses.Review;
import play.Logger;
import play.cache.Cache;
import service.ReviewSource;
import service.reviews.ReviewFetcher;
import twitter4j.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Collections2.filter;

public class TwitterService implements ReviewFetcher {
    @Nullable private Date lastMessageDate;

    public void setLastMessageDate(@Nullable Date messageDate) {
        lastMessageDate = messageDate;
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        if (lastMessageDate != null) {
            /* This is a streaming request. */
            List<Review> reviews;
            int i = 0;
            while ((reviews = getReviewsFiltered(lastMessageDate, business)).size() == 0 && (i++ < 20)) {
                try {
                    Thread.sleep(750L);
                } catch (InterruptedException e) {
                    Logger.warn("Thread interrupted in getReviews!");
                }
            }
            return reviews;
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
        Logger.info("Message date: %s", lastMessageDate);
        return new ArrayList<Review>(filter(getReviewsActual(business), new IsDateRecent(lastMessageDate)));
    }

    private List<Review> getReviewsActual(@Nonnull Business business) {
        if (business.name == null) {
            return null;
        }
        boolean lookupDistance = business.latitude != null && business.longitude != null;

        List<Review> reviews = new ArrayList<Review>();
        Twitter twitter = new TwitterFactory().getInstance();
        try {
            QueryResult result = twitter.search(new Query(business.name));
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
        return reviews;
    }

    private static class IsDateRecent implements Predicate<Review> {
        private Date lastMessageDate;

        private IsDateRecent(Date lastMessageDate) {
            this.lastMessageDate = lastMessageDate;
        }

        @Override
        public boolean apply(@Nullable Review review) {
            return review != null && review.date != null && lastMessageDate.compareTo(review.date) < 0;
        }
    }
}
