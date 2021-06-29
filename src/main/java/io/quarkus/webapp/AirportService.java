package io.quarkus.webapp;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import static javax.transaction.Transactional.TxType.REQUIRED;
import static javax.transaction.Transactional.TxType.SUPPORTS;
import java.util.List;

@ApplicationScoped
@Transactional(REQUIRED)
public class AirportService {

    @Transactional(SUPPORTS)
    public Airport findRandomAirport() {
        return Airport.findRandom();
    }

    @Transactional(SUPPORTS)
    public List<Airport> getAirports() {
        return Airport.listAll();
    }

}
