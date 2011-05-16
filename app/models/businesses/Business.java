package models.businesses;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Inheritance;

import static javax.persistence.InheritanceType.JOINED;

@Entity
@Inheritance(strategy=JOINED)
public class Business extends Model {
    public Double latitude;
    public Double longitude;
    public String name;
    public String address;
    public String city;
    public String state;
    public String zip;
    public String phone;
    public String yelpId;
    public String facebookId;
    public String twitterId;
    public String urbanspoonId;
    public String tripAdvisorId;

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
