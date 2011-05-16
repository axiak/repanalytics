package service.yelp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.yelp.v2.YelpSearchResult;
import models.businesses.Business;
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
import play.libs.WS;
import service.PhoneBusinessSearcher;
import service.RemoteBusinessFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static play.libs.Codec.hexMD5;


public class YelpV2API implements YelpAPI, RemoteBusinessFinder, PhoneBusinessSearcher {
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
            mBusiness.address = Joiner.on(", ").skipNulls().join(object.get("address1"), object.get("address2"), object.get("address3"));
            mBusiness.city = object.get("city").getAsString();
            mBusiness.latitude = object.get("latitude").getAsDouble();
            mBusiness.longitude = object.get("longitude").getAsDouble();
            mBusiness.name = object.get("name").getAsString();
            mBusiness.phone = object.get("phone").getAsString();
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
            mBusiness.phone = business.getPhone();
            mBusiness.state = business.getLocation().getStateCode();
            mBusiness.zip = business.getLocation().getPostalCode();
            mBusiness.yelpId = business.getId();
            mBusiness.childYelpId = business.getId();
            businesses.add(mBusiness);
        }
        Cache.set(cacheKey, businesses, "1440mn");
        return businesses;
    }
}
