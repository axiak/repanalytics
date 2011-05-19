package service.facebook;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Page;
import models.businesses.Business;
import models.businesses.Review;
import play.Logger;
import play.cache.Cache;
import service.reviews.ReviewFetcher;
import service.search.PhoneBusinessSearcher;
import service.search.RemoteBusinessFinder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static play.libs.Codec.hexMD5;
import static util.Strings.normalizePhone;

public class FacebookService implements RemoteBusinessFinder, ReviewFetcher, PhoneBusinessSearcher {
    @Override
    public List<Business> findByPhone(String phone) {
        phone = normalizePhone(phone);
        String cacheKey = "facebook_phone_" + hexMD5(phone);
        @SuppressWarnings("unchecked")
        List<Business> result = Cache.get(cacheKey, List.class);
        if (result != null) {
            return result;
        }
        FacebookClient facebookClient = new DefaultFacebookClient();

        JsonObject response = facebookClient.fetchObject("search", JsonObject.class, Parameter.with("phone", phone),
                                                          Parameter.with("type", "page"));
        result = handlePageResponse(response);
        Cache.set(cacheKey, result, "1440mn");
        return result;
    }

    @Override
    public List<Business> findBusinessesByName(String name, double lat, double lng, int distance) {
        String cacheKey = "facebook_page_" + hexMD5(name + "," + lat + "," + lng + "," + distance);
        @SuppressWarnings("unchecked")
        List<Business> result = Cache.get(cacheKey, List.class);
        if (result != null) {
            return result;
        }

        FacebookClient facebookClient = new DefaultFacebookClient();

        JsonObject response = facebookClient.fetchObject("search", JsonObject.class, Parameter.with("q", name),
                                                          Parameter.with("type", "page"),
                                                          Parameter.with("location", "" + lat + "," + lng));
        result = handlePageResponse(response);
        Cache.set(cacheKey, result, "1440mn");
        return result;
    }

    private List<Business> handlePageResponse(JsonObject response) {
        return new ArrayList<Business>();
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        return new ArrayList<Review>();
    }
}
