package models.businesses;

import play.db.jpa.Model;

import javax.persistence.Entity;

@Entity
public class Business extends Model {
    public Double latitude;
    public Double longitude;
    public String name;
    public String address;
    public String city;
    public String state;
    public String zip;
    public String phone;

}
