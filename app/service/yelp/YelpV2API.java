package service.yelp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yelp.v2.YelpSearchResult;
import models.businesses.Business;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.cache.Cache;
import service.RemoteBusinessFinder;
import util.SimpleMD5;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static util.SimpleMD5.md5hex;


public class YelpV2API implements YelpAPI, RemoteBusinessFinder {
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private static final String YELP_CONSUMER_KEY = "V9q5yxBx0CbP842Ndbf7Ww",
            YELP_SECRET_KEY = "Luy2nmHdBtrrckjEvXD0UT472uk",
            YELP_TOKEN = "dvleDISwNCZi3yf1LdFyvReAOfJh0vuC",
            YELP_SECRET_TOKEN = "zdNkkuyUY-gMUO7EzHp4IzoufvU";

    @Override
    public YelpSearchResult getYelpSearchResults(Map<String, String> params) {
        OAuthService service = new ServiceBuilder()
                .provider(YelpV2OauthAPI.class)
                .apiKey(YELP_CONSUMER_KEY)
                .apiSecret(YELP_SECRET_KEY)
                .build();
        Token accessToken = new Token(YELP_TOKEN, YELP_SECRET_TOKEN);

        OAuthRequest request = new OAuthRequest(Verb.GET,
                "http://api.yelp.com/v2/search");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            request.addQuerystringParameter(entry.getKey(), entry.getValue());
        }
        service.signRequest(accessToken, request);

        Response response = request.send();
        return gson.fromJson(response.getBody(),
                             YelpSearchResult.class);
    }

    @Override
    public List<Business> findBusinessesByNameAndPhone(String name, double lat, double lng, int distance) {
        String cacheKey = "yelp_" +
                          md5hex("" + lat + "," + lng + "," + distance + "," + name.toLowerCase().trim());

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
            Business mBusiness = new Business();
            mBusiness.address = Joiner.on(", ").skipNulls().join(business.getLocation().getAddress());
            mBusiness.city = business.getLocation().getCity();
            mBusiness.latitude = business.getLocation().getCoordinate().getLatitude();
            mBusiness.longitude = business.getLocation().getCoordinate().getLongitude();
            mBusiness.name = business.getName();
            mBusiness.phone = business.getPhone();
            mBusiness.state = business.getLocation().getStateCode();
            mBusiness.zip = business.getLocation().getPostalCode();
            businesses.add(mBusiness);
        }
        System.out.println(cacheKey);
        Cache.set(cacheKey, businesses, "1440mn");
        return businesses;
    }
}
