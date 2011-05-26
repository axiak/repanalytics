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
import service.twitter.TwitterRealtimeFetcher;
import util.TimestampGsonSerializer;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class DemoStreamWebSocket extends WebSocketController {
    public static RealtimeReviewFetcher reviewFetcher = TwitterRealtimeFetcher.getInstance();

    public static void getReviewStream(final String id) {
        Logger.info("Initiating web socket request...");
        final Business business = Demo.getBusinessById(id);
        final Gson pollGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new TimestampGsonSerializer())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        reviewFetcher.startBusiness(business);
        while (inbound.isOpen()) {
            final F.Either<Http.WebSocketEvent, List<Review>> event = await(F.Promise.waitEither(
                    inbound.nextEvent(),
                    reviewFetcher.getReviewsOnReady(business)));

            if (event._2 != null && event._2.isDefined() && event._2.get().size() > 0) {
                final List<Review> reviews = event._2.get();
                Collections.sort(reviews);
                outbound.send(pollGson.toJson(reviews));
            }

            for (Http.WebSocketClose closed : Http.WebSocketEvent.SocketClosed.match(event._1)) {
                reviewFetcher.shutdown(business);
                disconnect();
            }
        }
    }
}
