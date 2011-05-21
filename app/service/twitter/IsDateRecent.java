package service.twitter;

import com.google.common.base.Predicate;
import models.businesses.Review;

import javax.annotation.Nullable;
import java.util.Date;

public class IsDateRecent implements Predicate<Review> {
    private Date lastMessageDate;

    public IsDateRecent(Date lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    @Override
    public boolean apply(@Nullable Review review) {
        return review != null && review.date != null && lastMessageDate.compareTo(review.date) < 0;
    }
}
