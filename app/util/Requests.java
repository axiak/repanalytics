package util;

import bootstrap.JmxInitialization;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import play.Logger;
import play.Play;
import play.mvc.Http;

import java.io.File;
import java.io.IOException;

import static com.maxmind.geoip.LookupService.GEOIP_MEMORY_CACHE;

public final class Requests {
    private static LookupService geoIp = null;

    private static void initializeGeoIp() {
        String path = new File(new File(Play.applicationPath, "dat"), "GeoIPCity.dat").getAbsolutePath();
        try {
            geoIp = new LookupService(path, GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            Logger.info(e, "Could not open geoip service.");
        }
    }

    public static String getIpAddress(Http.Request request) {
        String ip = null;
        Http.Header forwardedFor = request.headers.get("x-forwarded-for");
        if (forwardedFor == null) {
            ip = request.remoteAddress;
        } else {
            ip = forwardedFor.value().split(",")[0];
        }
        if (ip.startsWith("127.") || ip.startsWith("192.168.")) {
            ip = Play.configuration.getProperty("test.ip.address", "209.113.164.2");
        }
        return ip;
    }

    public static Location getRequestLocation(Http.Request request) {
        if (geoIp == null) {
            initializeGeoIp();
        }
        return geoIp.getLocation(getIpAddress(request));
    }
}
