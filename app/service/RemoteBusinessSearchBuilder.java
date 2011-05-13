package service;

import com.google.code.geocoder.Geocoder;
import com.google.code.geocoder.GeocoderRequestBuilder;
import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderRequest;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.LatLng;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.primitives.Doubles;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import models.businesses.Business;
import play.Logger;
import play.cache.Cache;
import play.libs.F;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

import static util.SimpleMD5.md5hex;

public final class RemoteBusinessSearchBuilder {
    RemoteBusinessFinder service;
    Map<String, String> searchParameters;


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

    private List<F.Tuple<Double, Business>> sortResults(List<Business> results) {
        List<F.Tuple<Double, Business>> data = new ArrayList<F.Tuple<Double, Business>>(Collections2.transform(results, new Function<Business, F.Tuple<Double, Business>>() {
            @Override
            public F.Tuple<Double, Business> apply(@Nullable Business business) {
                double total = 1;
                for (Map.Entry<String, String> entry : searchParameters.entrySet()) {
                    String key = entry.getKey();
                    if (normalizeField(key, entry.getValue()).equals(
                        normalizeField(key, getBusinessField(business, key)))) {
                        total *= 0.2;
                    } else {
                        total *= 0.8;
                    }
                }
                return new F.Tuple<Double, Business>(1 - total, business);
            }
        }));
        Collections.sort(data, new Comparator<F.Tuple<Double, Business>>(){
            @Override
            public int compare(F.Tuple<Double, Business> doubleBusinessTuple, F.Tuple<Double, Business> doubleBusinessTuple1) {
                return Doubles.compare(doubleBusinessTuple1._1, doubleBusinessTuple._1);
            }
        });
        return data;
    }

    private String getBusinessField(Business business, String parameter) {
        try {
            Field field = Business.class.getField(parameter);
            return (String)field.get(business);
        } catch (NoSuchFieldException e) {
            Logger.info(e, "Issue getting parameter from Business object.");
        } catch (IllegalAccessException e) {
            Logger.info(e, "Issue getting parameter from Business object.");
        }
        return "";
    }

    private String normalizeField(String fieldName, String value) {
        if (value == null) {
            return "";
        } else if ("phone".equals(fieldName)) {
            try {
                PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                return pnu.format(pnu.parse(value, "US"), PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            } catch (NumberParseException e) {
                return value.trim().toLowerCase(Locale.getDefault());
            }
        } else {
           return value.trim().toLowerCase(Locale.getDefault());
        }

    }

    private List<Double> getCoordinates() {
        String addressLine = Joiner.on(", ").skipNulls().join(Arrays.asList(get("address"), get("city"), get("state")));
        String cacheKey = "geocode_" + md5hex(addressLine);
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
