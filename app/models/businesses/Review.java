package models.businesses;

import play.db.jpa.Model;
import service.ReviewSource;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames = {"source", "date", "userName", "rating"}))
public class Review extends Model {
    public Business business;
    public Double sentiment;
    @Enumerated(EnumType.STRING)
    public ReviewSource source;
    public String userName;
    @Temporal(TemporalType.DATE)
    public Date date;
    @Column(columnDefinition="TEXT")
    public String text;
    public Integer rating;

}
