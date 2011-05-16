package controllers;


import com.google.common.collect.ImmutableMap;
import models.registration.SeedUser;
import play.Logger;
import play.mvc.Controller;

import static util.Requests.getIpAddress;

public class Registration extends Controller {

    public static void registerEmail() {
        String email = request.params.get("email");
        validation.email(email);
        if (validation.errorsMap().size() > 0) {
            Logger.debug("Invalid email address: '%s'", email);
            renderJSON(ImmutableMap.<String, String>of("status", "invalid"));
        }
        try {
            SeedUser user = new SeedUser();
            user.email = email;
            user.ipAddress = getIpAddress(request);
            user.userAgent = (request.headers.get("user-agent") == null) ? "" : request.headers.get("user-agent").value();
            user.save();
            renderJSON(ImmutableMap.<String, String>of("status", "success"));
        } catch (Throwable t) {
            Logger.warn(t, "Could not save seed user.");
            renderJSON(ImmutableMap.<String, String>of("status", "failed"));
        }
    }

}
