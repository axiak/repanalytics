package service;

import models.businesses.Business;

import java.util.List;

public interface PhoneBusinessSearcher {
    public List<Business> findByPhone(String phone);
}
