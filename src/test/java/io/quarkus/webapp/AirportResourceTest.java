package io.quarkus.webapp;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;


@Testcontainers
@QuarkusTest
public class AirportResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello!"));
    }

    @Test
    public void getRandomAirports() {
        given()
                .when().get("/airports/random")
                .then()
                .statusCode(is(200));
    }
}