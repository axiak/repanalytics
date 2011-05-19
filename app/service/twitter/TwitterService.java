package service.twitter;


import models.businesses.Business;
import models.businesses.Review;
import play.Logger;
import service.reviews.ReviewFetcher;
import twitter4j.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TwitterService implements ReviewFetcher {
    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        Logger.warn("Entering twitter job!");
        if (business.name == null) {
            return null;
        }
        boolean lookupDistance = business.latitude != null && business.longitude != null;

        Twitter twitter = new TwitterFactory().getInstance();
        try {
            QueryResult result = twitter.search(new Query(business.name));
            for (Tweet tweet : result.getTweets()) {
                GeoLocation loc = tweet.getGeoLocation();
                if (lookupDistance && loc != null) {
                    Logger.info("Tweet info: %s,%s", loc.getLatitude(), loc.getLongitude());
                }
            }
        } catch (TwitterException e) {
            Logger.error(e, "Could not search twitter.");
        }
        return new ArrayList<Review>();
    }
}
