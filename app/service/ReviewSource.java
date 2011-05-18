package service;

public enum ReviewSource {

    YELP("yelp"),
    URBANSPOON("urbanspoon"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    TRIP_ADVISOR("tripAdvisor");

    private String name;

    ReviewSource(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
