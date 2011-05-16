package controllers;

import bootstrap.JmxInitialization;
import play.mvc.Controller;

public class Simple extends Controller {
    public static void index() {
        new JmxInitialization().now();
        render();
    }
}
