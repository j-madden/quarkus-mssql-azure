package io.quarkus.webapp;

import org.jboss.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
@Produces(APPLICATION_JSON)
public class AirportResource {

    private static final Logger LOGGER = Logger.getLogger(AirportResource.class);

    @Inject
    AirportService service;

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello!";
    }

    @GET
    @Path("airports/{ident}")
    public Response getAirportByIdent(@PathParam("ident") String ident){
        Airport airport = Airport.getAirportByIdent(ident);
        if(airport != null){
            LOGGER.info("Found airport with ident="+ident+"\n"+airport);
            return Response.ok(airport).build();
        }
        else{
            LOGGER.info("Could not find airport with ident="+ident);
            return Response.noContent().build();
        }
    }

    @Path("airports/random")
    @GET
    @Produces(APPLICATION_JSON)
    public Response getRandomAirport() {
        Airport airport = service.findRandomAirport();
        LOGGER.info("Found random airport " + airport);
        return Response.ok(airport).build();
    }
}