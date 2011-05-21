package controllers;

import play.mvc.Controller;
import service.twitter.TwitterOfflineJobs;

public class OfflineJobs extends Controller {
    public static void getTwitterInfo() {
        await(new TwitterOfflineJobs().now());
        renderText("DONE");
    }


}
