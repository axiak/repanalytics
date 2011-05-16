package models.businesses;


import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class YelpBusiness extends Business {
    @Column(unique=true)
    public String childYelpId;
}
