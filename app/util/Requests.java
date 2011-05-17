package util;

import play.Logger;
import play.Play;
import play.mvc.Http;

public final class Requests {
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
}
