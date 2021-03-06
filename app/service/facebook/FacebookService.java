package service.facebook;

import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Post;
import models.businesses.Business;
import models.businesses.Review;
import play.cache.Cache;
import service.ReviewSource;
import service.reviews.ReviewFetcher;
import service.search.PhoneBusinessSearcher;
import service.search.RemoteBusinessFinder;

import javax.annotation.Nonnull;
import java.util.*;

import static play.libs.Codec.hexMD5;
import static util.Strings.normalizePhone;

public class FacebookService implements RemoteBusinessFinder, ReviewFetcher, PhoneBusinessSearcher {
    @Override
    public List<Business> findByPhone(String phone) {
        if (Strings.isNullOrEmpty(phone)) {
            return new ArrayList<Business>();
        }

        final String normalizedPhone = normalizePhone(phone);
        final String cacheKey = "facebook_phone_" + hexMD5(normalizedPhone);
        @SuppressWarnings("unchecked")
        List<Business> result = Cache.get(cacheKey, List.class);
        if (result == null) {
            FacebookClient facebookClient = new DefaultFacebookClient();

            JsonObject response = facebookClient.fetchObject("search", JsonObject.class, Parameter.with("phone", normalizedPhone),
                                                              Parameter.with("type", "page"));
            result = handlePageResponse(response);
            Cache.set(cacheKey, result, "1440mn");
        }
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

        final FacebookClient facebookClient = new DefaultFacebookClient();

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
        Set<String> reviewTexts = new HashSet<String>();
        for (Post post : publicSearch.getData()) {
            if (post == null || post.getMessage() == null || reviewTexts.contains(post.getMessage())) {
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
            reviewTexts.add(post.getMessage());
        }
        Collections.shuffle(reviews);
        reviews = new ArrayList<Review>(reviews.subList(0, Math.min(10, reviews.size())));
        Cache.set(cacheKey, reviews, "1440mn");
        return reviews;
    }
}
