package controllers;

import bootstrap.JmxInitialization;
import com.google.gson.reflect.TypeToken;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import models.businesses.Business;
import models.businesses.BusinessChain;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F;
import play.mvc.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import service.search.RemoteBusinessSearchBuilder;
import service.yelp.YelpV2API;

import static com.google.common.collect.Collections2.transform;
import static com.maxmind.geoip.LookupService.GEOIP_MEMORY_CACHE;
import static play.libs.Codec.hexMD5;
import static util.Requests.getIpAddress;

public class Demo extends Controller {
    private static LookupService geoIp = null;
    private static final String[] STATE_CODES = "AL,AK,AS,AZ,AR,CA,CO,CT,DE,DC,FM,FL,GA,GU,HI,ID,IL,IN,IA,KS,KY,LA,ME,MH,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,MP,OH,OK,OR,PW,PA,PR,RI,SC,SD,TN,TX,UT,VT,VI,VA,WA,WV,WI,WY".split(",");

    private static void initializeGeoIp() {
        String path = new File(new File(Play.applicationPath, "dat"), "GeoIPCity.dat").getAbsolutePath();
        try {
            geoIp = new LookupService(path, GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            Logger.error(e, "Could not open geoip service.");
        }
    }


    public static void index() {
        new JmxInitialization().now();
        if (geoIp == null) {
            initializeGeoIp();
        }
        Location l = geoIp.getLocation(getIpAddress(request));
        renderArgs.put("states", STATE_CODES);
        renderArgs.put("currentState", l.region);
        render();
    }

    public static void name(String term) {
        String cacheKey = "name_l_" + hexMD5(term);
        @SuppressWarnings("unchecked")
        List<String> results = Cache.get(cacheKey, List.class);
        if (results == null) {
            List<BusinessChain> chains = BusinessChain.find("byNameIlike", "%" + term + "%").fetch();
            results = new ArrayList<String>(transform(chains, BusinessChain.nameGetter()));
            if (results.size() > 15) {
                results = new ArrayList<String>();
            }
            Cache.set(cacheKey, results, "1440min");
        }
        renderJSON(results);
    }

    public static void search() {
        RemoteBusinessSearchBuilder rbsb = new RemoteBusinessSearchBuilder(new YelpV2API());

        List<F.Tuple<Double, Business>> businesses = await(rbsb
                .name(request.params.get("name"))
                .city(request.params.get("city"))
                .address(request.params.get("address"))
                .state(request.params.get("state"))
                .phone(request.params.get("phone"))
                .now());

        renderJSON(businesses, new TypeToken<List<F.Tuple<Double, Business>>>(){}.getType());
    }


}