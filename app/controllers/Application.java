package controllers;

import models.businesses.Business;
import play.*;
import play.libs.F;
import play.mvc.*;

import java.util.*;

import models.*;
import service.RemoteBusinessSearchBuilder;
import service.yelp.YelpV2API;

public class Application extends Controller {

    public static void index() {
        RemoteBusinessSearchBuilder rbsb = new RemoteBusinessSearchBuilder(new YelpV2API());

        List<F.Tuple<Double, Business>> businesses = rbsb
                .name("Cheesecake Factory")
                .city("Cambridge")
                .address("100 Cambridgeside Place")
                .state("MA")
                .search();
        renderArgs.put("businesses", businesses);
        render();
    }

}