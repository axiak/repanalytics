package controllers;

import bootstrap.JmxInitialization;
import play.mvc.Before;
import play.mvc.Controller;

public class Simple extends Controller {
    @Before
    public static void initializeJmx() {
        new JmxInitialization().now();
    }

    public static void index() {
        initializeJmx();
        render();
    }

    public static void notready() {
        initializeJmx();
        render();
    }
}
