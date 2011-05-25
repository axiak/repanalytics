package service.reviews;

import models.businesses.Business;
import models.businesses.Review;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The ReviewFetcher class
 */
public interface ReviewFetcher {
    public List<Review> getReviews(@Nonnull Business business);
}
