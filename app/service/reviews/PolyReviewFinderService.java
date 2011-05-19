package service.reviews;

import com.google.common.base.Function;
import models.businesses.Business;
import models.businesses.Review;
import play.jobs.Job;
import play.libs.F;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Collections2.transform;

public final class PolyReviewFinderService extends Job<Map<Business, List<Review>>> {
    private List<ReviewFetcher> reviewFetchers;
    private List<Business> businesses;

    public PolyReviewFinderService(@Nonnull List<ReviewFetcher> fetchers, Business ... businesses) {
        this.reviewFetchers = fetchers;
        this.businesses = Arrays.asList(businesses);
    }

    @Override
    public Map<Business, List<Review>> doJobWithResult() throws Exception {
        Collection<F.Promise<Map<Business, List<Review>>>> finderServices = transform(reviewFetchers,
                new FetcherToPromise(businesses)
                );
        @SuppressWarnings("unchecked")
        List<Map<Business, List<Review>>> result =  (List)F.Promise.waitAll(finderServices.toArray(new F.Promise[finderServices.size()])).get();
        return mergeResults(result);
    }

    public static Map<Business, List<Review>> mergeResults(List<Map<Business, List<Review>>> results) {
        Map<Business, List<Review>> aggregate = new HashMap<Business, List<Review>>();
        for (Map<Business, List<Review>> sourceList : results) {
            if (sourceList == null) {
                continue;
            }
            for (Map.Entry<Business, List<Review>> entry : sourceList.entrySet()) {
                if (!aggregate.containsKey(entry.getKey())) {
                    aggregate.put(entry.getKey(), new ArrayList<Review>(entry.getValue()));
                } else {
                    aggregate.get(entry.getKey()).addAll(entry.getValue());
                }
            }
        }
        // TODO - If this (O(n ln n)) is too slow, we can merge the sorted arrays in O(n) above
        for (List<Review> reviewList : aggregate.values()) {
            Collections.sort(reviewList);
        }
        return aggregate;
    }

    private static class FetcherToPromise implements Function<ReviewFetcher, F.Promise<Map<Business, List<Review>>>> {
        private Business[] businesses;
        private FetcherToPromise(List<Business> businesses) {
            this.businesses = businesses.toArray(new Business[businesses.size()]);
        }

        @Override
        public F.Promise<Map<Business, List<Review>>> apply(@Nullable ReviewFetcher reviewFetcher) {
            if (reviewFetcher == null) {
                return null;
            }
            return new ReviewFinderService(reviewFetcher, businesses).now();
        }
    }
}
