package service;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.LatLng;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.primitives.Doubles;
import models.businesses.Business;
import play.cache.Cache;
import play.libs.F;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static play.libs.Codec.hexMD5;

public final class RemoteBusinessSearchBuilder {
    RemoteBusinessFinder service;
    Map<String, String> searchParameters;
    private final static ExecutorService executorService = Executors.newCachedThreadPool();

    public RemoteBusinessSearchBuilder(RemoteBusinessFinder service) {
        this.service = service;
        this.searchParameters = new HashMap<String, String>();
    }

    public RemoteBusinessSearchBuilder name(String name) {
        this.searchParameters.put("name", name);
        return this;
    }

    public RemoteBusinessSearchBuilder phone(String phone) {
        this.searchParameters.put("phone", phone);
        return this;
    }

    public RemoteBusinessSearchBuilder address(String address) {
        this.searchParameters.put("address", address);
        return this;
    }

    public RemoteBusinessSearchBuilder zip(String zip) {
        this.searchParameters.put("zip", zip);
        return this;
    }

    public RemoteBusinessSearchBuilder city(String city) {
        this.searchParameters.put("city", city);
        return this;
    }

    public RemoteBusinessSearchBuilder state(String state) {
        this.searchParameters.put("state", state);
        return this;
    }


    public List<F.Tuple<Double, Business>> search() {
        List<Double> coordinates = getCoordinates();

        List<Business> results = service.findBusinessesByNameAndPhone(get("name"),
                                                                      coordinates.get(0),
                                                                      coordinates.get(1),
                                                                      20);
        return sortResults(results);
    }

    public Future<List<F.Tuple<Double, Business>>> searchAsync() {
        return executorService.submit(new Callable<List<F.Tuple<Double, Business>>>(){
            @Override
            public List<F.Tuple<Double, Business>> call() throws Exception {
                return search();
            }
        });
    }

    private List<F.Tuple<Double, Business>> sortResults(List<Business> results) {
        List<F.Tuple<Double, Business>> data = new ArrayList<F.Tuple<Double, Business>>(Collections2.transform(results, new ProbabilisticMatcher(searchParameters)));
        Collections.sort(data, new Comparator<F.Tuple<Double, Business>>(){
            @Override
            public int compare(F.Tuple<Double, Business> doubleBusinessTuple, F.Tuple<Double, Business> doubleBusinessTuple1) {
                return Doubles.compare(doubleBusinessTuple1._1, doubleBusinessTuple._1);
            }
        });
        return data;
    }

    private List<Double> getCoordinates() {
        String addressLine = Joiner.on(", ").skipNulls().join(Arrays.asList(get("address"), get("city"), get("state")));
        String cacheKey = "geocode_" + hexMD5(addressLine);
        @SuppressWarnings("unchecked")
        List<Double> result = Cache.get(cacheKey, List.class);
        if (result != null) {
            return result;
        }
        GeocoderRequest request = new GeocoderRequestBuilder().setAddress(addressLine).getGeocoderRequest();

        GeocodeResponse response = new Geocoder().geocode(request);
        List<GeocoderResult> addresses = response.getResults();

        if (addresses.size() == 0) {
            throw new IllegalArgumentException("Cannot find address provided.");
        }
        LatLng latlng = addresses.get(0).getGeometry().getLocation();
        result = Arrays.asList(latlng.getLat().doubleValue(), latlng.getLng().doubleValue());
        Cache.set(cacheKey, result, "1440mn");
        return result;
    }

    private String get(String parameter) {
        return searchParameters.get(parameter);
    }
}
