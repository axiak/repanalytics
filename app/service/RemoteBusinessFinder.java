package service;

import models.businesses.Business;

import java.util.List;

public interface RemoteBusinessFinder {
    public List<Business> findBusinessesByName(String name, double lat, double lng, int distance);
}
