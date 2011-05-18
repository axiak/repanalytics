package models.businesses;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.PersistenceException;

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

    /*
     * This calls save if the id is null. If there is a duplicate pk due to the source,
     * it will try and find the actual replacement.
     */
    public Business addToDatabase() {
        if (this.id != null) {
            return this;
        }
        try {
            this.save();
            return this;
        } catch (PersistenceException e) {
            if (this.yelpId != null) {
                return YelpBusiness.find("byChildYelpId", this.yelpId).<Business>first();
            }
            /* TODO - As we add more sources, we should uncomment this logic. */
            /*
            else if (this.facebookId != null) {
                return FacebookBusiness.find("byChildFacebookId", this.facebookId).<Business>first();
            }
            else if (this.urbanspoonId != null) {
                return UrbanspoonBusiness.find("byChildUrbanspoonId", this.urbanspoonId).<Business>first();
            }
            else if (this.tripAdvisorId != null) {
                return TripAdvisorBusiness.find("byChildTripAdvisorId", this.tripAdvisorId).<Business>first();
            }
            else if (this.twitterId != null) {
                return twitterBusiness.find("byChildTwitterId", this.twitterId).<Business>first();
            }
            */
            else {
                throw e;
            }
        }
    }
}
