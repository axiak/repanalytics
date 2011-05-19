package service.search;


import com.google.common.base.Function;
import models.businesses.Business;
import play.jobs.Job;
import play.libs.F;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Collections2.transform;

public final class PolyRemoteBusinessSearchBuilder extends Job<List<F.Tuple<Double, Business>>> {
    List<RemoteBusinessSearchBuilder> searchBuilders;

    public PolyRemoteBusinessSearchBuilder(@Nonnull RemoteBusinessFinder... finders) {
        List<RemoteBusinessSearchBuilder> builders = new ArrayList<RemoteBusinessSearchBuilder>(finders.length);
        for (RemoteBusinessFinder finder : finders) {
            if (finder != null) {
                builders.add(new RemoteBusinessSearchBuilder(finder));
            }
        }
        this.searchBuilders = builders;
    }
    

    public PolyRemoteBusinessSearchBuilder name(String name) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.name(name);
        }
        return this;
    }
    
    public PolyRemoteBusinessSearchBuilder phone(String phone) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.phone(phone);
        }
        return this;
    }

    public PolyRemoteBusinessSearchBuilder address(String address) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.address(address);
        }
        return this;
    }
    
    public PolyRemoteBusinessSearchBuilder zip(String zip) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.zip(zip);
        }
        return this;
    }
    
    public PolyRemoteBusinessSearchBuilder city(String city) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.city(city);
        }
        return this;
    }
    
    public PolyRemoteBusinessSearchBuilder state(String state) {
        for (RemoteBusinessSearchBuilder builder : searchBuilders) {
            builder.state(state);
        }
        return this;
    }
    @Override
    public List<F.Tuple<Double, Business>> doJobWithResult() throws Exception {
        @SuppressWarnings("unchecked")
        List<List<F.Tuple<Double, Business>>> results = (List)F.Promise.waitAll(
                transform(searchBuilders, new FinderToPromise()).toArray(new F.Promise[searchBuilders.size()])
                ).get();
        return mergeResults(results);
    }

    private List<F.Tuple<Double, Business>> mergeResults(List<List<F.Tuple<Double, Business>>> results) {
        /* Dumb merging strategy: Union */
        List<F.Tuple<Double, Business>> businesses = new ArrayList<F.Tuple<Double, Business>>();
        for (List<F.Tuple<Double, Business>> currentBusinesses : results) {
            if (currentBusinesses != null) {
                businesses.addAll(currentBusinesses);
            }
        }
        return businesses;
    }

    private static class FinderToPromise implements Function<RemoteBusinessSearchBuilder, F.Promise<List<F.Tuple<Double, Business>>>> {
        @Override
        public F.Promise<List<F.Tuple<Double, Business>>> apply(@Nullable RemoteBusinessSearchBuilder builder) {
            if (builder == null) {
                return null;
            }
            return builder.now();
        }
    }
}
