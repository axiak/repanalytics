package service.yelp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.sun.deploy.net.BasicHttpRequest;
import com.yelp.v2.YelpSearchResult;
import models.businesses.Business;
import models.businesses.YelpBusiness;
import oauth.signpost.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.Logger;
import play.Play;
import play.cache.Cache;
import service.RemoteBusinessFinder;
import sun.net.www.http.HttpClient;

import java.io.IOException;
import java.net.URL;
import java.sql.ClientInfoStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static play.libs.Codec.hexMD5;


public class YelpV2API implements YelpAPI, RemoteBusinessFinder {
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Override
    public YelpSearchResult getYelpSearchResults(Map<String, String> params) {
        OAuthService service = new ServiceBuilder()
                .provider(YelpV2OauthAPI.class)
                .apiKey(Play.configuration.getProperty("yelp.consumer.key"))
                .apiSecret(Play.configuration.getProperty("yelp.secret.key"))
                .build();

        Token accessToken = new Token(Play.configuration.getProperty("yelp.token"),
                                      Play.configuration.getProperty("yelp.secret.token"));

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

    private List<Business> findByPhone(String phone) {
        /* Use phone number search here.
        HttpRequest request = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
        String normalizedPhone;
        try {
            normalizedPhone = PhoneNumberUtil.getInstance().format(PhoneNumberUtil.getInstance().parse(phone, "US"),
                                                                   PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            normalizedPhone = phone;
        }
        try {
            HttpClient client = HttpClient.New(new URL("http://api.yelp.com/phone_search"));
            client.
        } catch (IOException e) {
            Logger.warn("Failed to create httpclient.", e);
            return null;
        }
        */
        return new ArrayList<Business>();
    }



    @Override
    public List<Business> findBusinessesByNameAndPhone(String name, double lat, double lng, int distance) {
        String cacheKey = "yelp_" +
                          hexMD5("" + lat + "," + lng + "," + distance + "," + name.toLowerCase().trim());

        @SuppressWarnings("unchecked")
        List<Business> businesses = null; //Cache.get(cacheKey, List.class);
        if (businesses != null) {
            return businesses;
        }

        YelpSearchResult result = getYelpSearchResults(ImmutableMap.<String, String>of(
                "term", name,
                "radius_filter", String.valueOf(distance),
                //"category_filter", "restaurants",
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
            mBusiness.phone = business.getPhone();
            mBusiness.state = business.getLocation().getStateCode();
            mBusiness.zip = business.getLocation().getPostalCode();
            mBusiness.yelpId = business.getId();
            mBusiness.childYelpId = business.getId();
            businesses.add(mBusiness);
        }
        System.out.println(cacheKey);
        Cache.set(cacheKey, businesses, "1440mn");
        return businesses;
    }
}
