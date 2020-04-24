package io.quarkus.webapp;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import java.util.Random;

@Entity
public class Airport extends PanacheEntity {

    public String ident;
    public String type;
    public String name;
    public Float latitude_deg;
    public Float longitude_deg;
    public int elevation_ft;
    public String continent;
    public String iso_country;
    public String iso_region;
    public String municipality;
    public String scheduled_service;
    public String gps_code;
    public String iata_code;
    public String local_code;
    public String home_link;
    public String wikipedia_link;
    public String keywords;


    public static Airport getAirportByIdent(String identifier) {
        return find("ident", identifier).firstResult();
    }

    @Override
    public String toString() {
        return "Airport{" +
            "id=" + id + '\'' +
            ", ident=" + ident + '\'' +
            ", type='" + type + '\'' +
            ", name='" + name + '\'' +
            ", latitude_deg=" + latitude_deg + '\'' +
            ", longitude_deg='" + longitude_deg + '\'' +
            ", elevation_ft=" + elevation_ft + '\'' +
            ", continent='" + continent + '\'' +
            ", iso_country='" + iso_country + '\'' +
            ", iso_country=" + iso_country + '\'' +
            ", iso_region='" + iso_region + '\'' +
            ", municipality=" + municipality + '\'' +
            ", scheduled_service='" + scheduled_service + '\'' +
            ", gps_code='" + gps_code + '\'' +
            ", iata_code=" + iata_code + '\'' +
            ", local_code=" + local_code + '\'' +
            ", home_link=" + home_link + '\'' +
            ", wikipedia_link=" + wikipedia_link + '\'' +
            ", keywords=" + keywords + '\'' +
            '}';
    }

    public static Airport findRandom() {
        long countAirports = Airport.count();
        Random random = new Random();
        int randomAirport = random.nextInt((int) countAirports);
        return Airport.findAll().page(randomAirport, 1).firstResult();
    }

}
