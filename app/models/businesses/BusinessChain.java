package models.businesses;

import play.Play;
import play.db.jpa.Model;

import javax.persistence.Entity;

@Entity
public class BusinessChain extends Model {
    String name;

    public BusinessChain(String name) {
        this.name = name;
    }
}
