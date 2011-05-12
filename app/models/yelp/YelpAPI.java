package models.yelp;


import com.yelp.v2.YelpSearchResult;

import java.util.Map;

public interface YelpAPI {
    public YelpSearchResult getYelpSearchResults(Map<String, String> params);
}
