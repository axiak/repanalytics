package controllers;

import bootstrap.JmxInitialization;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.maxmind.geoip.Location;
import models.businesses.Business;
import models.businesses.BusinessChain;
import models.businesses.Review;
import play.Play;
import play.cache.Cache;
import play.libs.F;
import play.mvc.*;

import java.io.File;
import java.util.*;

import service.facebook.FacebookService;
import service.reviews.PolyReviewFinderService;
import service.reviews.RealtimeReviewFetcher;
import service.reviews.ReviewFetcher;
import service.reviews.ReviewFinderService;
import service.search.PolyRemoteBusinessSearchBuilder;
import service.twitter.TwitterRealtimeFetcher;
import service.twitter.TwitterService;
import service.yelp.YelpV2API;
import util.TimestampGsonSerializer;

import javax.annotation.Nullable;

import static com.google.common.collect.Collections2.transform;
import static play.libs.Codec.hexMD5;
import static util.NaturalLanguages.trainClassifier;
import static util.Requests.getRequestLocation;

public final class Demo extends Controller {
    private static final String[] STATE_CODES = "AL,AK,AS,AZ,AR,CA,CO,CT,DE,DC,FM,FL,GA,GU,HI,ID,IL,IN,IA,KS,KY,LA,ME,MH,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,MP,OH,OK,OR,PW,PA,PR,RI,SC,SD,TN,TX,UT,VT,VI,VA,WA,WV,WI,WY".split(",");
    public static RealtimeReviewFetcher reviewFetcher = TwitterRealtimeFetcher.getInstance();

    @Before
    public static void initializeJmx() {
        new JmxInitialization().now();
    }

    public static void index() {
        initializeJmx();
        final Location location = getRequestLocation(request);
        renderArgs.put("states", STATE_CODES);
        renderArgs.put("currentState", location.region);
        render();
    }

    public static void name(final String term) {
        initializeJmx();
        final String cacheKey = "name_l_" + hexMD5(term);
        @SuppressWarnings("unchecked")
        List<String> results = Cache.get(cacheKey, List.class);
        if (results == null) {
            final List<BusinessChain> chains = BusinessChain.find("byNameIlike", "%" + term + "%").fetch();
            results = new ArrayList<String>(transform(chains, BusinessChain.nameGetter()));
            if (results.size() > 15) {
                results = new ArrayList<String>();
            }
            Cache.set(cacheKey, results, "1440min");
        }
        renderJSON(results);
    }

    public static void search() {
        initializeJmx();
        for (String param : Arrays.asList("name", "city", "address", "state", "phone")) {
            if (request.params.get(param) == null) {
                redirect("/demo/");
            }
        }
        final PolyRemoteBusinessSearchBuilder rbsb = new PolyRemoteBusinessSearchBuilder(new YelpV2API(), new FacebookService());

        final List<F.Tuple<Double, Business>> businesses = await(
                rbsb
                        .name(request.params.get("name"))
                        .city(request.params.get("city"))
                        .address(request.params.get("address"))
                        .state(request.params.get("state"))
                        .phone(request.params.get("phone"))
                .now());

        int index = -1;
        for (F.Tuple<Double, Business> business : businesses) {
            index++;
            final Business currentBusiness = business._2.addToDatabase();
            businesses.set(index, new F.Tuple<Double, Business>(business._1, currentBusiness));
            Cache.set("business_" + currentBusiness.id, currentBusiness);
        }

        if (businesses.isEmpty()) {
            renderJSON(new F.Tuple<String, String>("notfound", ""), new TypeToken<F.Tuple<String, String>>(){}.getType());
        }

        if (businesses.get(0) != null && Math.abs(businesses.get(0)._1 - 1.000) < 0.00001) {
            demoInformation(businesses.get(0)._2);
        }

        renderJSON(new F.Tuple<String, List<F.Tuple<Double, Business>>>("disambiguate", businesses),
                   new TypeToken<F.Tuple<String, List<F.Tuple<Double, Business>>>>() {}.getType());
    }

    public static void pollTwitterInfo(String id, final long lastDate) {
        Integer maxReviews = request.params.get("feedMaxSize", Integer.class);
        TwitterService twitterService = new TwitterService();
        Business business = getBusinessById(id);
        List<Review> reviews;

        if (lastDate == 0) {
            twitterService.setReviewMinDate(lastDate);
            twitterService.setMaxReviews(maxReviews == null ? 8 : maxReviews);
            ReviewFinderService service = new ReviewFinderService(twitterService, business);
            reviews = await(service.now()).get(business);
        } else {
            reviewFetcher.startBusiness(business);
            F.Either<List<Review>, F.Timeout> result = await(F.Promise.waitEither(
                    reviewFetcher.getReviewsOnReady(business),
                    F.Timeout("1min")));
            if (result._2.isDefined()) {
                reviews = new ArrayList<Review>();
                reviewFetcher.cancelPromise(business);
            } else {
                reviews = result._1.get();
            }
        }

        Collections.sort(reviews);
        Gson pollGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new TimestampGsonSerializer())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        renderJSON(pollGson.toJson(reviews));
    }


    public static void demoInformation(String id) {
        if (!request.isAjax()) {
            renderTemplate("Demo/index.html", id);
        }
        demoInformation(getBusinessById(id));
    }

    private static void demoInformation(Business business) {
        List<ReviewFetcher> finders = new ArrayList<ReviewFetcher>();
        finders.add(new YelpV2API());
        finders.add(new FacebookService());
        PolyReviewFinderService service = new PolyReviewFinderService(finders,
                                                                      business);
        List<Review> reviews = await(service.now()).get(business);
        renderJSON(new GsonBuilder().setDateFormat("MM/dd/yyyy").create().toJson(reviews));
    }

    public static Business getBusinessById(String id) {
        Business business = Cache.get("business_" + id, Business.class);
        if (business == null) {
            business = Business.find("byId", Long.valueOf(id)).<Business>first();
            Cache.set("business_" + id, business);
        }
        return business;
    }

    private static class IsBusinessWellMatched implements Predicate<F.Tuple<Double, Business>> {
        @Override
        public boolean apply(@Nullable F.Tuple<Double, Business> doubleBusinessTuple) {
            return doubleBusinessTuple != null && doubleBusinessTuple._1 > 0.7;
        }
    }

    public static void trainModel() {
        trainClassifier(new File("/tmp/data"),
                        new File(new File(Play.applicationPath, "dat"), "en-classifier.bin"));
        renderText("Success!");
    }
}