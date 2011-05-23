package controllers;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import models.businesses.Business;
import models.businesses.Review;
import play.Logger;
import play.libs.F;
import play.mvc.Http;
import play.mvc.WebSocketController;
import service.reviews.RealtimeReviewFetcher;
import service.reviews.ReviewFinderService;
import service.twitter.TwitterRealtimeFetcher;
import service.twitter.TwitterService;
import util.TimestampGsonSerializer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DemoStreamWebSocket extends WebSocketController {
    public static RealtimeReviewFetcher reviewFetcher = new TwitterRealtimeFetcher();

    public static void getReviewStream(String id) {
        Logger.info("Initiating web socket request...");
        Business business = Demo.getBusinessById(id);
        reviewFetcher.startBusiness(business);
        while (inbound.isOpen()) {
            F.Either<Http.WebSocketEvent, List<Review>> e = await(F.Promise.waitEither(
                    inbound.nextEvent(),
                    reviewFetcher.getReviewsOnReady(business)));

            if (e._2 != null && e._2.isDefined() && e._2.get().size() > 0) {
                List<Review> reviews = e._2.get();
                Collections.sort(reviews);
                Gson pollGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new TimestampGsonSerializer())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
                outbound.send(pollGson.toJson(reviews));
            }

            for (Http.WebSocketClose closed : Http.WebSocketEvent.SocketClosed.match(e._1)) {
                reviewFetcher.shutdown(business);
                disconnect();
            }
        }
    }
}
