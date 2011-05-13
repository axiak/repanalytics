package models.businesses;

import com.google.common.base.Objects;
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

    @Override
    public String toString() {
        return "Business{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
