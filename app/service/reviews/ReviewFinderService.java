package service.reviews;

import models.businesses.Business;
import models.businesses.Review;
import play.jobs.Job;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.NaturalLanguages.reviewSentiment;

public class ReviewFinderService extends Job<Map<Business, List<Review>>> {
    private ReviewFetcher fetcher;
    private List<Business> businesses;

    public ReviewFinderService(@Nonnull ReviewFetcher fetcher, Business... businesses) {
        this.fetcher = fetcher;
        this.businesses = Arrays.asList(businesses);
    }

    @Override
    public Map<Business, List<Review>> doJobWithResult() throws Exception {
        Map<Business, List<Review>> result = new HashMap<Business, List<Review>>(businesses.size());
        for (Business business : businesses) {
            List<Review> reviews = fetcher.getReviews(business);
            for (Review review : reviews) {
                reviewSentiment(review);
            }
            result.put(business, reviews);
        }
        return result;
    }
}
