package io.quarkus.webapp;

import io.quarkus.test.junit.NativeImageTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;


@NativeImageTest
public class NativeAirportResourceIT extends AirportResourceTest {

    // Execute the same tests but in native mode.
    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }
}