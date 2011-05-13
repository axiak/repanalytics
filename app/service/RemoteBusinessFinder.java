package service;

import models.businesses.Business;

import java.util.List;

public interface RemoteBusinessFinder {
    public List<Business> findBusinessesByNameAndPhone(String name, double lat, double lng, int distance);
}
