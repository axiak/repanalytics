package controllers;

import bootstrap.JmxInitialization;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import models.businesses.Business;
import models.businesses.BusinessChain;
import models.businesses.Review;
import models.businesses.YelpBusiness;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.F;
import play.mvc.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import service.facebook.FacebookService;
import service.reviews.PolyReviewFinderService;
import service.reviews.ReviewFetcher;
import service.reviews.ReviewFinderService;
import service.search.PolyRemoteBusinessSearchBuilder;
import service.search.RemoteBusinessSearchBuilder;
import service.twitter.TwitterService;
import service.yelp.YelpV2API;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;

import static com.google.common.collect.Collections2.transform;
import static com.maxmind.geoip.LookupService.GEOIP_MEMORY_CACHE;
import static play.libs.Codec.hexMD5;
import static util.NaturalLanguages.trainClassifier;
import static util.Requests.getIpAddress;
import static util.Requests.getRequestLocation;

public class Demo extends Controller {
    private static final String[] STATE_CODES = "AL,AK,AS,AZ,AR,CA,CO,CT,DE,DC,FM,FL,GA,GU,HI,ID,IL,IN,IA,KS,KY,LA,ME,MH,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,MP,OH,OK,OR,PW,PA,PR,RI,SC,SD,TN,TX,UT,VT,VI,VA,WA,WV,WI,WY".split(",");

    @Before
    public static void initializeJmx() {
        new JmxInitialization().now();
    }

    public static void index() {
        Location l = getRequestLocation(request);
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

        for (String param : Arrays.asList("name", "city", "address", "state", "phone")) {
            if (request.params.get(param) == null) {
                redirect("/demo/");
            }
        }
        PolyRemoteBusinessSearchBuilder rbsb = new PolyRemoteBusinessSearchBuilder(new YelpV2API(), new FacebookService());

        List<F.Tuple<Double, Business>> businesses = await(
                rbsb
                        .name(request.params.get("name"))
                        .city(request.params.get("city"))
                        .address(request.params.get("address"))
                        .state(request.params.get("state"))
                        .phone(request.params.get("phone"))
                .now());

        int i=0;
        for (F.Tuple<Double, Business> business : businesses) {
            Business currentBusiness = business._2.addToDatabase();
            businesses.set(i, new F.Tuple<Double, Business>(business._1, currentBusiness));
            Cache.set("business_" + currentBusiness.id, currentBusiness);
            i++;
        }

        if (businesses.size() == 0) {
            renderJSON(new F.Tuple<String, String>("notfound", ""), new TypeToken<F.Tuple<String, String>>(){}.getType());
            return;
        }

        if (businesses.get(0) != null && Math.abs(businesses.get(0)._1 - 1.000) < 0.00001) {
            demoInformation(businesses.get(0)._2);
            return;
        }

        renderJSON(new F.Tuple<String, List<F.Tuple<Double, Business>>>("disambiguate", businesses),
                   new TypeToken<F.Tuple<String, List<F.Tuple<Double, Business>>>>() {}.getType());
    }

    public static void demoInformation(String id) {
        if (!request.isAjax()) {
            renderTemplate("Demo/index.html", id);
        }
        Business business = Cache.get("business_" + id, Business.class);
        if (business == null) {
            business = Business.find("byId", Long.valueOf(id)).<Business>first();
        }
        demoInformation(business);
    }

    private static void demoInformation(Business business) {
        List<ReviewFetcher> finders = new ArrayList<ReviewFetcher>();
        finders.add(new YelpV2API()); finders.add(new FacebookService()); finders.add(new TwitterService());
        PolyReviewFinderService service = new PolyReviewFinderService(finders,
                                                                      business);
        List<Review> reviews = await(service.now()).get(business);
        renderJSON(new GsonBuilder().setDateFormat("MM/dd/yyyy").create().toJson(reviews));
    }


    private static class IsBusinessWellMatched implements Predicate<F.Tuple<Double, Business>> {
        @Override
        public boolean apply(@Nullable F.Tuple<Double, Business> doubleBusinessTuple) {
            return doubleBusinessTuple != null && doubleBusinessTuple._1 > 0.7;
        }
    }

    public static void trainModel() {
        trainClassifier();
        renderText("Success!");
    }
}