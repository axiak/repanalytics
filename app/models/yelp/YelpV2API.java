package models.yelp;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yelp.v2.YelpSearchResult;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.Map;


public class YelpV2API implements YelpAPI {
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private static final String YELP_CONSUMER_KEY = "V9q5yxBx0CbP842Ndbf7Ww",
            YELP_SECRET_KEY = "Luy2nmHdBtrrckjEvXD0UT472uk",
            YELP_TOKEN = "dvleDISwNCZi3yf1LdFyvReAOfJh0vuC",
            YELP_SECRET_TOKEN = "zdNkkuyUY-gMUO7EzHp4IzoufvU";

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
        System.out.println(response.getBody());
        return gson.fromJson(response.getBody(),
                             YelpSearchResult.class);
    }
}
