package controllers;

import models.businesses.Business;
import play.*;
import play.libs.F;
import play.mvc.*;

import java.util.*;
import service.RemoteBusinessSearchBuilder;
import service.yelp.YelpV2API;

public class Application extends Controller {

    public static void index() {
        RemoteBusinessSearchBuilder rbsb = new RemoteBusinessSearchBuilder(new YelpV2API());

        List<F.Tuple<Double, Business>> businesses = await(rbsb
                .name("Cheesecake Factory")
                .city("Cambridge")
                .address("100 Cambridgeside Place")
                .state("MA")
                .phone("6172523810")
                .searchAsync());

        renderArgs.put("businesses", businesses);
        render();
    }

}