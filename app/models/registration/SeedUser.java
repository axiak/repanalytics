package models.registration;

import play.db.jpa.JPABase;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
public class SeedUser extends Model {
    @Column(unique=true) public String email;
    @Temporal(TemporalType.TIMESTAMP) public Date insertDate;
    public String ipAddress;
    public String userAgent;

    @Override
    public <T extends JPABase> T save() {
        if (insertDate == null) {
            insertDate = new Date();
        }
        return super.<T>save();
    }
}
