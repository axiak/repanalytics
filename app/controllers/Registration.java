package controllers;


import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import models.registration.SeedUser;
import play.Logger;
import play.mvc.Controller;

import static util.Requests.getIpAddress;

public final class Registration extends Controller {

    public static void registerEmail() {
        final String email = request.params.get("email");
        final String numberLocationsParam = request.params.get("locations");

        int numberLocations = 0;
        if (!Strings.isNullOrEmpty(numberLocationsParam)) {
            try {
                numberLocations = Integer.valueOf(numberLocationsParam);
            } catch (IllegalArgumentException e) {
                numberLocations = 0;
            }
        }

        final String name = request.params.get("name");

        validation.email(email);
        if (validation.errorsMap().size() > 0) {
            Logger.debug("Invalid email address: '%s'", email);
            renderJSON(ImmutableMap.<String, String>of("status", "invalid"));
        }
        try {
            final SeedUser user = new SeedUser();
            user.email = email;
            user.fullName = name;
            user.ipAddress = getIpAddress(request);
            user.userAgent = (request.headers.get("user-agent") == null) ? "" : request.headers.get("user-agent").value();
            user.numberLocations = numberLocations;
            user.save();
            renderJSON(ImmutableMap.<String, String>of("status", "success"));
        } catch (Throwable t) {
            Logger.warn(t, "Could not save seed user.");
            renderJSON(ImmutableMap.<String, String>of("status", "failed"));
        }
    }

}
