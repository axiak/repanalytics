package models.businesses;

import com.google.gson.annotations.Expose;
import play.db.jpa.Model;
import service.ReviewSource;

import javax.persistence.*;
import javax.print.attribute.IntegerSyntax;
import java.util.Date;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames = {"source", "date", "userName", "rating"}))
public class Review extends Model implements Comparable<Review> {
    public Business business;
    @Expose
    public Double sentiment;
    @Expose
    @Enumerated(EnumType.STRING)
    public ReviewSource source;
    @Expose
    public String sourceId;
    @Expose
    public String sourceUrl;
    @Expose
    public String userName;
    @Temporal(TemporalType.DATE)
    @Expose
    public Date date;
    @Column(columnDefinition="TEXT")
    @Expose
    public String text;
    @Expose
    public Integer rating;

    @Override
    public int compareTo(Review review) {
        /* Default order is descending by date. */
        Date thisDate = date == null ? new Date(0) : date;
        Date otherDate = review.date == null ? new Date(0) : review.date;
        return -thisDate.compareTo(otherDate);
    }
}
