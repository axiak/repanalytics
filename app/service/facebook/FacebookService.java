package service.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import models.businesses.Business;
import models.businesses.Review;
import org.h2.store.Page;
import play.Logger;
import service.reviews.ReviewFetcher;
import service.search.RemoteBusinessFinder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FacebookService implements RemoteBusinessFinder, ReviewFetcher {
    @Override
    public List<Business> findBusinessesByName(String name, double lat, double lng, int distance) {
        FacebookClient facebookClient = new DefaultFacebookClient();
        Page response = facebookClient.fetchObject("search", Page.class, Parameter.with("q", name),
                        Parameter.with("type", "page"), Parameter.with("location", "" + lat + "," + lng));
        Logger.info("Response from facebook: %s", response);
        return new ArrayList<Business>();
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        return new ArrayList<Review>();
    }
}
