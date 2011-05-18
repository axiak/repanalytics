package service.yelp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.yelp.v2.YelpSearchResult;
import models.businesses.Business;
import models.businesses.Review;
import models.businesses.YelpBusiness;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F;
import play.libs.WS;
import service.ReviewSource;
import service.reviews.ReviewFetcher;
import service.search.PhoneBusinessSearcher;
import service.search.RemoteBusinessFinder;

import javax.annotation.Nonnull;
import java.util.*;

import static play.libs.Codec.hexMD5;
import static util.Strings.normalizePhone;


public class YelpV2API implements YelpAPI, RemoteBusinessFinder, PhoneBusinessSearcher, ReviewFetcher {
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private F.Tuple<OAuthService, Token> getYelpOAuthInfo() {
        OAuthService service = new ServiceBuilder()
                .provider(YelpV2OauthAPI.class)
                .apiKey(Play.configuration.getProperty("yelp.consumer.key"))
                .apiSecret(Play.configuration.getProperty("yelp.secret.key"))
                .build();

        Token accessToken = new Token(Play.configuration.getProperty("yelp.token"),
                                      Play.configuration.getProperty("yelp.secret.token"));
        return new F.Tuple<OAuthService, Token>(service, accessToken);
    }


    @Override
    public YelpSearchResult getYelpSearchResults(Map<String, String> params) {
        F.Tuple<OAuthService, Token> oAuthInfo = getYelpOAuthInfo();

        OAuthRequest request = new OAuthRequest(Verb.GET,
                "http://api.yelp.com/v2/search");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.addQuerystringParameter(entry.getKey(), entry.getValue());
        }
        oAuthInfo._1.signRequest(oAuthInfo._2, request);

        Response response = request.send();
        return gson.fromJson(response.getBody(),
                             YelpSearchResult.class);
    }

    @Override
    public List<Business> findByPhone(String phone) {
        String normalizedPhone;
        try {
            normalizedPhone = PhoneNumberUtil.getInstance().format(PhoneNumberUtil.getInstance().parse(phone, "US"),
                                                                   PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            normalizedPhone = phone;
        }
        String cacheKey = "yelp_phone_" + hexMD5(normalizedPhone);
        @SuppressWarnings("unchecked")
        String resultString = Cache.get(cacheKey, String.class);
        if (resultString == null) {
            WS.HttpResponse response = WS.url("http://api.yelp.com/phone_search?phone=%s&ywsid=%s",
                                              normalizedPhone, Play.configuration.getProperty("yelp.ywsid")).get();
            resultString = response.getString();
            Cache.set(cacheKey, resultString, "1440min");
        }

        JsonElement result = new JsonParser().parse(resultString);

        List<Business> businesses = new ArrayList<Business>(1);
        for (JsonElement element : result.getAsJsonObject().get("businesses").getAsJsonArray()) {
            JsonObject object = element.getAsJsonObject();
            YelpBusiness mBusiness = new YelpBusiness();
            mBusiness.address = Joiner.on(", ").skipNulls().join(makeEmptyNull(object.get("address1").getAsString()),
                    makeEmptyNull(object.get("address2").getAsString()),
                    makeEmptyNull(object.get("address3").getAsString()));
            mBusiness.city = object.get("city").getAsString();
            mBusiness.latitude = object.get("latitude").getAsDouble();
            mBusiness.longitude = object.get("longitude").getAsDouble();
            mBusiness.name = object.get("name").getAsString();
            mBusiness.phone = normalizePhone(object.get("phone").getAsString());
            mBusiness.state = object.get("state").getAsString();
            mBusiness.zip = object.get("zip").getAsString();
            mBusiness.childYelpId = mBusiness.yelpId = object.get("id").getAsString();
            businesses.add(mBusiness);
        }
        return businesses;
    }

    @Override
    public List<Business> findBusinessesByName(String name, double lat, double lng, int distance) {
        String cacheKey = "yelp_" +
                          hexMD5("" + lat + "," + lng + "," + distance + "," + name.toLowerCase().trim());

        @SuppressWarnings("unchecked")
        List<Business> businesses = Cache.get(cacheKey, List.class);
        if (businesses != null) {
            return businesses;
        }

        YelpSearchResult result = getYelpSearchResults(ImmutableMap.<String, String>of(
                "term", name,
                "radius_filter", String.valueOf(distance),
                "category_filter", "restaurants",
                "ll", Joiner.on(",").join(lat, lng),
                "limit", "20"));

        businesses = new ArrayList<Business>();
        for (com.yelp.v2.Business business : result.getBusinesses()) {
            YelpBusiness mBusiness = new YelpBusiness();
            mBusiness.address = Joiner.on(", ").skipNulls().join(business.getLocation().getAddress());
            mBusiness.city = business.getLocation().getCity();
            mBusiness.latitude = business.getLocation().getCoordinate().getLatitude();
            mBusiness.longitude = business.getLocation().getCoordinate().getLongitude();
            mBusiness.name = business.getName();
            mBusiness.phone = normalizePhone(business.getPhone());
            mBusiness.state = business.getLocation().getStateCode();
            mBusiness.zip = business.getLocation().getPostalCode();
            mBusiness.yelpId = business.getId();
            mBusiness.childYelpId = business.getId();
            businesses.add(mBusiness);
        }
        Cache.set(cacheKey, businesses, "1440mn");
        return businesses;
    }

    private Object makeEmptyNull(Object input) {
        if (input != null && input.toString().trim().length() == 0) {
            return null;
        } else {
            return input;
        }
    }

    @Override
    public List<Review> getReviews(@Nonnull Business business) {
        if (business.yelpId == null) {
            return new ArrayList<Review>();
        }
        String response = Cache.get("yelp_reviews_" + business.yelpId, String.class);
        if (response == null) {
            F.Tuple<OAuthService, Token> oAuthInfo = getYelpOAuthInfo();
            OAuthRequest request = new OAuthRequest(Verb.GET,
                    "http://api.yelp.com/v2/business/" + business.yelpId);
            oAuthInfo._1.signRequest(oAuthInfo._2, request);

            response = request.send().getBody();
        }

        Cache.set("yelp_reviews_" + business.yelpId, response, "1440mn");

        JsonElement element = new JsonParser().parse(response);

        JsonArray jsonReviews = element.getAsJsonObject().get("reviews").getAsJsonArray();
        List<Review> reviews = new ArrayList<Review>();

        for (JsonElement reviewElement : jsonReviews) {
            JsonObject reviewObject = reviewElement.getAsJsonObject();
            Review review = new Review();
            review.business = business;
            try {
                review.date = new Date(1000L * reviewObject.get("time_created").getAsInt());
                review.rating = reviewObject.get("rating").getAsInt();
                review.text = reviewObject.get("excerpt").getAsString();
                review.userName = reviewObject.get("user").getAsJsonObject().get("name").getAsString();
                review.source = ReviewSource.YELP;
            } catch (NullPointerException e) {
                Logger.info(e, "");
                Logger.info("Failed to parse json response from yelp for business %s: %s", business.id, response);
                continue;
            }
            reviews.add(review);
        }

        return reviews;
    }
}
