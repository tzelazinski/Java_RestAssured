package utilities;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * This class contains reusable requests that serve different testing purposes.
 */
public class RestRequests {

    /**
     * This function sends the request and returns the response containing JSON
     * format data of beers brewed after specified date.
     *
     * @param date desired date  in format MM-YYYY
     * @return response
     */
    public static Response getBeersBrewedAfter(String date) {

        return given()
                .log()
                .all()
                .contentType(ContentType.JSON)
                .queryParam("brewed_after", date)
                .queryParam("per_page", "80")
                .get();
    }

    /**
     * This function sends the request and returns the response containing JSON
     * format data of one full page of all beers
     *
     * @param page desired results page
     * @return response
     */
    public static Response getAllBeersSpecificPage(Integer page) {

        return given()
                .log()
                .uri()
                .contentType(ContentType.JSON)
                .queryParam("page", page)
                .queryParam("per_page", "80")
                .get();
    }

    /**
     * This function sends the request and returns the response containing JSON
     * format data of one full page of all beers with one parameter
     *
     * @param page     desired results page
     * @param operator parameter for api data filtering in request
     * @param trigger  int value of parameter
     * @return response
     */
    public static Response getAllBeersFilterByOneParamIntTriggerSpecificPage(Integer page, String operator, Integer trigger) {

        return given()
                .log()
                .uri()
                .contentType(ContentType.JSON)
                .queryParam(operator, trigger)
                .queryParam("page", page)
                .queryParam("per_page", "80")
                .get();
    }

    /**
     * This function sends the request and returns the response containing JSON
     * format data of one full page of all beers with one parameter
     *
     * @param page     desired results page
     * @param operator parameter for api data filtering in request
     * @param trigger  string value of parameter
     * @return response
     */
    public static Response getAllBeersFilterByOneParamStringTriggerSpecificPage(Integer page, String operator, String trigger) {

        return given()
                .log()
                .uri()
                .contentType(ContentType.JSON)
                .queryParam(operator, trigger)
                .queryParam("page", page)
                .queryParam("per_page", "80")
                .get();
    }
}
