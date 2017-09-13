import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;

public class WebdamAPI {


    @BeforeClass
    public static void initialize() {
        // Set defaults
        RestAssured.baseURI = "http://interview-testing-api.webdamdb.com";
    }

    @Before
    public void RefreshToken() {
        String token = getToken();
        RestAssured.requestSpecification = givenWithContent(token);
    }

    @Test
    public void validateLogin() {
        given()
                .when()
                    .get("/login")
                .then()
                    .log()
                    .ifValidationFails()
                    .statusCode(202)
                    .body("logged_in", is(true));
    }

    @Test
    // Note test will fail. Bug in log out, should report logged_in == false
    public void validateLogOff() {
        given()
                .when()
                .get("/logout")
                .then()
                .log()
                .ifValidationFails()
                .statusCode(200)
                .body("logged_in", is(false));
    }

    // Fetch auth token. Will be invoked before each test call
    public String getToken() {
        RestAssured.basePath = "";
        RestAssured.requestSpecification = null;

        String access_token = given().formParam("grant_type", "client_credentials")
                .formParam("client_id", "4")
                .formParam("client_secret", "4ovGa5yXfHnWR47wGRVUfKlDTBxC3WQtnkmO5sgs")
                .when()
                // Move this to config
                    .post("/oauth/token")
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("expires_in", lessThanOrEqualTo(3600))
                    .body("token_type", equalTo("Bearer"))
                    .extract().path("access_token");

        return access_token;
    }

    // Helper function to set auth token in each request.
    public static RequestSpecification givenWithContent(String authToken) {
        RequestSpecification spec = given().spec(new RequestSpecBuilder()
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .build());
        if (authToken != null) {
            return spec.header("Authorization", "Bearer " + authToken);
        } else {
            return spec;
        }
    }




}
