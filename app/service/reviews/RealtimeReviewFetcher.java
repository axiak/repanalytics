package service.reviews;

import models.businesses.Business;
import models.businesses.Review;
import play.libs.F;

import javax.annotation.Nonnull;
import java.util.List;

public interface RealtimeReviewFetcher {
    public void startBusiness(@Nonnull Business business);
    public F.Promise<List<Review>> getReviewsOnReady(@Nonnull Business business);
    public void cancelPromise(@Nonnull Business business);
    public void shutdown(@Nonnull Business business);
    public void cleanOldBusinesses(String timeout);
}
