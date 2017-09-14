import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
/*
README
1)Possible improved to catch bug add compare to schema for example. Certain assets have date_created > datemodified.
    If this is important field we should add validation
2) Token refresh is not added for now. It should be handled by test code.
3) Bug max limit param is 10 but when greater value is added to limit it returns 0 results, Ideally it should return max 10 results
    Example http://interview-testing-api.webdamdb.com/api/v1/search?query=&sort=asc&limit=11 should return 10 values
 */

public class WebdamAPI {


    @BeforeClass
    public static void initialize() {
        // Set defaults
        RestAssured.baseURI = "http://interview-testing-api.webdamdb.com";
    }

    @Before
    public void RefreshToken() {
        // Since there is not concept of users this should work.
        // Token refresh logic can be added to getToken() to handle any token expiry.
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


    @Test
    // Search for invalid or missing data and make sure not contents are received.
    // Matcher size checks for empty body content
    public void searchNonEixistingAssets() {
        given()
                .when()
                    .param("query","thisdoesnotexist")
                    .param("sort", "asc")
                    .param("limit",0)
                    .get("/search")
                .then()
                    .statusCode(200)
                    .body("", Matchers.hasSize(0));
    }

    @Test
    // Search for text which has whitepace before and after text. Currently there is a bug which will fail this test case
    // For example search for text old does not return any results. It should have returned assetid 12342
    public void searchAssetsWithWhiteSpaces() {
        given()
                .when()
                    .param("query","old")
                    .param("sort", "asc")
                    .param("limit",0)
                    .get("/search")
                .then()
                    .statusCode(200)
                    .body("", Matchers.hasSize(1));
    }

    @Test
    //Verify that the response fetched for a particular keyword is correct and related to the keyword
    // Use json schema validator for better validation
    public void searchAssetWithFixedQuery() {
        given()
                .when()
                    .param("query","old man")
                    .param("sort", "asc")
                    .param("limit",1) //Make sure to get one record in the expected order
                    .get("/search")
                .then()
                    .statusCode(200)
                    .assertThat()
                    .body("asset_id", contains("12342"));
    }

    @Test
    //Verify that ascending sort works as expected
    public void searchandValidateSortingAsc() {
        String[] assetIDs = new String[] {"12341","12342","12343"};
        given()
                .when()
                    .param("query","")
                    .param("sort", "asc")
                    .param("limit",3)
                    .get("/search")
                .then()
                    .log()
                    .body()
                    .statusCode(200)
                    .assertThat()
                    .body("asset_id", contains(assetIDs));
    }

    @Test
    //Verify that descending sort works as expected
    public void searchandValidateSortingDsc() {
        String[] assetIDs = new String[] {"12343","12342","12341"};
        given()
                .when()
                    .param("query","")
                    .param("sort", "dsc")
                    .param("limit",3)
                    .get("/search")
                .then()
                    .statusCode(200)
                    .assertThat()
                    .body("asset_id", contains(assetIDs));
    }

    @Test
    //Verify limit works
    public void searchandValidateLimitParam() {
        String[] assetIDs = new String[] {"12341","12342"};
        given()
                .when()
                    .param("query","")
                    .param("sort", "asc")
                    .param("limit",2) //validating max 2 reponses
                    .get("/search")
                .then()
                    .statusCode(200)
                    .assertThat()
                .body("asset_id", contains(assetIDs));
    }

    @Test
    //Verify search response for a large but valid strings
    public void searchandValidateLargeStr() {
        given()
                .when()
                    .param("query","This cherry red boat from 1986 is one of a kind")
                    .param("sort", "asc")
                    .param("limit",0) //validating max 2 reponses
                    .get("/search")
                .then()
                    .statusCode(200)
                    .assertThat()
                    .body("asset_id", contains("12343"));
    }

    @Test
    //Verify asset end point.
    public void fetchAsset() {
        given()
                .when()
                  .get("/asset")
                .then()
                    .statusCode(200)
                .body("asset_id", hasSize(5));  //Checking for a fixed size of asset count. Will break if dataset changes
    }

    @Test
    //Verify assetID end point.
    public void fetchAssetAndAssetInformation() {
        given()
                .when()
                    .get("/asset/12341") //This can be changed to fetch asset api then fetch an asset from reponse. If data is not static or changing.
                .then()
                    .log()
                    .body()
                    .statusCode(200)
                    .body("asset_id", contains("12341"))
                //Just a sample showing validation of data returned. Can be extensive if needed
                    .body("text",contains("an apple a day keeps the doctor away"));

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
