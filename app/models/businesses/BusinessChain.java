package models.businesses;

import com.google.common.base.Function;
import play.Play;
import play.db.jpa.Model;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class BusinessChain extends Model {
    @Column(unique=true)
    String name;

    public BusinessChain(String name) {
        this.name = name;
    }

    public static Function<BusinessChain, String> nameGetter() {
        return new Function<BusinessChain, String>() {
            @Override
            public String apply(@Nullable BusinessChain businessChain) {
                return (businessChain == null) ? "" : businessChain.name;
            }
        };
    }
}
