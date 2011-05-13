package service.yelp;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yelp.v2.YelpSearchResult;

import java.util.Map;

public class YelpV2APIMock implements YelpAPI {
    private static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Override
    public YelpSearchResult getYelpSearchResults(Map<String, String> params) {
        String result = "{\"region\":{\"span\":{\"latitude_delta\":0.029291900000004034,\"longitude_delta\":0.022212300000006735},\"center\":{\"latitude\":30.348156500000002,\"longitude\":-87.1542295}},\"total\":40,\"businesses\":[{\"distance\":3538.6696861419723,\"mobile_url\":\"http://mobile.yelp.com/biz/3nhYVx5fKBgeZdJdiuWxPQ\",\"rating_img_url\":\"http://media1.ct.yelpcdn.com/static/201012164084228337/i/ico/stars/stars_4.png\",\"review_count\":1,\"name\":\"Sabine Sandbar\",\"rating_img_url_small\":\"http://media3.px.yelpcdn.com/static/20101216418129184/i/ico/stars/stars_small_4.png\",\"url\":\"http://www.yelp.com/biz/sabine-sandbar-gulf-breeze\",\"phone\":\"8509343141\",\"snippet_text\":\"Pretty neat place to hang out and loiter.  Mostly an older crowd and most (if not all) seem to be regulars and know each other.  This bar is on the second...\",\"snippet_image_url\":\"http://media4.ct.yelpcdn.com/photo/0fi2e-WYBjR_l_Et--gE3w/ms\",\"display_phone\":\"+1-850-934-3141\",\"id\":\"sabine-sandbar-gulf-breeze\",\"categories\":[[\"Nightlife\",\"nightlife\"]],\"location\":{\"city\":\"Gulf Breeze\",\"display_address\":[\"715 Pensacola Beach Blvd\",\"Gulf Breeze, FL 32561\"],\"geo_accuracy\":8,\"postal_code\":\"32561\",\"country_code\":\"US\",\"address\":[\"715 Pensacola Beach Blvd\"],\"coordinate\":{\"latitude\":30.334841999999998,\"longitude\":-87.144132999999997},\"state_code\":\"FL\"}}]}";
        return gson.fromJson(result,
                             YelpSearchResult.class);
    }
}
