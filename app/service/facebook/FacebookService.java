package service.facebook;

import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Page;
import com.restfb.types.Post;
import models.businesses.Business;
import models.businesses.Review;
import play.Logger;
import play.cache.Cache;
import service.ReviewSource;
import service.reviews.ReviewFetcher;
import service.search.PhoneBusinessSearcher;
import service.search.RemoteBusinessFinder;

import javax.annotation.Nonnull;
import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static play.libs.Codec.hexMD5;
import static util.Strings.normalizePhone;

public class FacebookService implements RemoteBusinessFinder, ReviewFetcher, PhoneBusinessSearcher {
    @Override
    public List<Business> findByPhone(String phone) {
        if (Strings.isNullOrEmpty(phone)) {
            return new ArrayList<Business>();
        }

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
        name = name.split(" - ")[0];
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
        if (business.name == null) {
            return new ArrayList<Review>();
        }
        String cacheKey = "facebook_review_" + business.id;
        @SuppressWarnings("unchecked")
        List<Review> reviews = Cache.get(cacheKey, List.class);
        if (reviews != null) {
            return reviews;
        }
        String name = business.name.split(" - ")[0];
        FacebookClient facebookClient = new DefaultFacebookClient();
        Connection<Post> publicSearch = facebookClient.fetchConnection("search", Post.class,
                                            Parameter.with("q", name), Parameter.with("type", "post"));
        reviews = new ArrayList<Review>();
        for (Post post : publicSearch.getData()) {
            if (post == null || post.getMessage() == null) {
                continue;
            }
            Review review = new Review();
            review.source = ReviewSource.FACEBOOK;
            review.text = post.getMessage();
            review.userName = post.getFrom().getName();
            review.date = post.getCreatedTime();
            review.business = business;
            review.sourceUrl = post.getLink();
            reviews.add(review);
            if (reviews.size() > 5) {
                break;
            }
        }
        Cache.set(cacheKey, reviews, "1440mn");
        return reviews;
    }
}
