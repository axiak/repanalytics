package models.businesses;

import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Review extends Model {
    public String userName;
    @Column(columnDefinition="TEXT")
    public String ratingText;
    public Integer rating;

}
