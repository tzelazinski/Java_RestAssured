import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import utilities.RestRequests;
import utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * This is a test suite class for Brew Dog challenge tests.
 */
public class BrewdogBeerChallenge {

    // In order to perform any test that would not only be based on data types, I need to make some assumptions.
    // 1. I assume that data returned by API is correct the way it is.
    // 2. I assume and confirmed that via email, that schema generated from response is correct.
    // 3. I assume that for API requests containing filtering of value being below or above, comparison to null values
    // should not be included. For example if beer instance has ibu=null, it should not be returned in api response for
    // https://api.punkapi.com/v2/beers?ibu_lt=60 request.
    // 4. I assume that some beers can have null ibu value provided as this may not be mandatory field.
    // Based on that assumptions I configured my tests accordingly and created 2 test data parameters to compare
    // against data returned by API.
    // They both provide a way of validating that API is returning complete set of data each time.

    final String BASE_URI = "https://api.punkapi.com/v2/";
    final String BASE_PATH = "beers";
    final int MAX_PER_PAGE = 80;

    final int EXPECTED_BEERS_COUNT = 325;
    final double EXPECTED_IBU_AVG = 65.24375;
    final String BREWED_AFTER_DATE = "12-2015";
    final int IBU_FILTER_GT = 100;
    final int IBU_FILTER_LT = 60;
    final String HOPS_FILTER = "Cascade";

    /**
     * Initialises base parameters for all the requests.
     */
    @BeforeClass
    public void setup() {

        baseURI = BASE_URI;
        basePath = BASE_PATH;
    }

    /**
     * This test case validates response status code and json schema.
     */
    @Test(priority = 1)
    public void testValidateSchema() {
        given()
                .queryParam("brewed_after", "12-2015")
                .queryParam("per_page", "80")
                .get()
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("schema.json"));
    }

    /**
     * This test validates that abv parameter for each beer brewed after 12-2015 is not null,
     * not an empty string and that it is greater than 4.0.
     * This test actually fails because one beer brewed after 12-2015 has abv<4.0
     */
    @Test(priority = 2)
    void testValidAbv() {

        Response res = RestRequests.getBeersBrewedAfter(BREWED_AFTER_DATE);
        JsonPath json = new JsonPath(res.asString());
        Map<Integer, Double> abvs_failed = new HashMap<>();
        boolean abv_null = false;

        for (int beer = 0; beer < json.getInt("size()"); beer++) {
            try {
                double abv = json.getDouble("abv[" + beer + "]"); //makes sure abv is not int, not null, not a string so not an empty string
                if (abv <= 4.0) {
                    int id = json.getInt("id[" + beer + "]");
                    abvs_failed.put(id, abv);
                }
            } catch (NullPointerException e) { //if id or abv null set variable to true
                abv_null = true;
            }
        }
        Assert.assertTrue(abvs_failed.isEmpty(), "For some beers abv value does not meet the condition abv>4.0: " + abvs_failed);
        Assert.assertFalse(abv_null, "At least one beer has abv value of null");
    }

    /**
     * This test validates that name parameter for each beer brewed after 12-2015 is not null or empty string.
     */
    @Test(priority = 3)
    void testValidName() {

        Response res = RestRequests.getBeersBrewedAfter(BREWED_AFTER_DATE);
        JsonPath json = new JsonPath(res.asString());
        Map<Integer, String> names_failed = new HashMap<>();
        boolean name_null = false;

        for (int beer = 0; beer < json.getInt("size()"); beer++) {
            try {
                String name = json.getString("name[" + beer + "]");
                if (name.isEmpty()) {
                    int id = json.getInt("id[" + beer + "]");
                    names_failed.put(id, name);
                }
            } catch (NullPointerException e) { //if name is null set variable to true
                name_null = true;
            }
        }
        Assert.assertTrue(names_failed.isEmpty(), "For some beers name is empty: " + names_failed);
        Assert.assertFalse(name_null, "At least one beer has name value of null");
    }

    /**
     * This test validates that each and every beer has a valid id which is integer and not null.
     */
    @Test(priority = 4)
    void testValidId() {

        int page = 1;
        int size = MAX_PER_PAGE;
        boolean id_null = false;

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersSpecificPage(page);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            for (int beer = 0; beer < size; beer++) {
                try {
                    json.getInt("id[" + beer + "]");
                } catch (NullPointerException e) { //if id is null set variable to true
                    id_null = true;
                }
            }
            page++;
        }
        Assert.assertFalse(id_null, "At least one beer has id value of null");
    }

    /**
     * This test validates that "ph" parameter has a valid value in proper range <0,14>.
     */
    @Test(priority = 5)
    void testValidPh() {

        Response res = RestRequests.getBeersBrewedAfter("12-2015");
        JsonPath json = new JsonPath(res.asString());
        Map<Integer, Double> phs_failed = new HashMap<>();
        boolean ph_null = false;

        for (int beer = 0; beer < json.getInt("size()"); beer++) {
            try {
                double ph = json.getDouble("ph[" + beer + "]");
                if (!(ph >= 0.0 && ph <= 14.0)) {
                    int id = json.getInt("id[" + beer + "]");
                    phs_failed.put(id, ph);
                }
            } catch (NullPointerException e) {
                ph_null = true;
            }
        }
        Assert.assertTrue(phs_failed.isEmpty(), "For some beers ph value is not in range <0,14>: " + phs_failed);
        Assert.assertFalse(ph_null, "At least one beer has ph value of null");
    }

    /**
     * This test asserts beer count against expected.
     */
    @Test(priority = 6)
    void testBeerCount() {

        int page = 1;
        int size = MAX_PER_PAGE;
        int count = 0;

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersSpecificPage(page);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            count += size;
            page++;
        }
        Assert.assertEquals(count, EXPECTED_BEERS_COUNT, "Beers count from API does not match expected.");
    }

    /**
     * This test asserts beers ibu average value against expected.
     */
    @Test(priority = 7)
    void testBeerIbuAvg() {

        int page = 1;
        int size = MAX_PER_PAGE;
        List<Double> ibus_values_list = new ArrayList<>();
        List<Integer> ibus_null_list = new ArrayList<>();

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersSpecificPage(page);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            for (int beer = 0; beer < size; beer++) {
                try {
                    double ibu = json.getDouble("ibu[" + beer + "]");
                    ibus_values_list.add(ibu);
                } catch (NullPointerException e) {
                    int id = json.getInt("id[" + beer + "]");
                    ibus_null_list.add(id);
                }
            }
            page++;
        }
        double ibu_avg = Utilities.countAverageFromList(ibus_values_list);
        if (!ibus_null_list.isEmpty()) {
            System.out.println("Information - Some beers have null value in ibu field: " + ibus_null_list);
        }
        Assert.assertEquals(ibu_avg, EXPECTED_IBU_AVG, "Beers ibu average count from API does not match expected.");
    }

    /**
     * This test validates API filtering over ibu values capability by comparing beers counts.
     * This test actually fails now because API treats null values as correct and returns beers
     * with nulls as meeting condition ibu_lt.
     */
    @Test(priority = 8)
    void testApiFilteringIbu() {

        int page = 1;
        int size = MAX_PER_PAGE;
        List<Double> ibus_above_100_list = new ArrayList<>();
        List<Double> ibus_below_60_list = new ArrayList<>();

        // Retrieve filtered values from all beers.
        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersSpecificPage(page);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            for (int beer = 0; beer < size; beer++) {
                try {
                    double ibu = json.getDouble("ibu[" + beer + "]");
                    if (ibu > IBU_FILTER_GT) {
                        ibus_above_100_list.add(ibu);
                    } else if (ibu < IBU_FILTER_LT) {
                        ibus_below_60_list.add(ibu);
                    }
                } catch (NullPointerException e) {
                    int id = json.getInt("id[" + beer + "]");
                    System.out.println("Beer id " + id + " has null ibu value");
                }
            }
            page++;
        }

        //Retrieve filtered values via api.
        page = 1;
        size = MAX_PER_PAGE;
        int count = 0;

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersFilterByOneParamIntTriggerSpecificPage(page, "ibu_gt", IBU_FILTER_GT);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            count += size;
            page++;
        }
        Assert.assertEquals(count, ibus_above_100_list.size(), "Beers count with ibu>100 from API does not match expected.");

        page = 1;
        size = MAX_PER_PAGE;
        count = 0;

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersFilterByOneParamIntTriggerSpecificPage(page, "ibu_lt", IBU_FILTER_LT);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            count += size;
            page++;
        }
        Assert.assertEquals(count, ibus_below_60_list.size(), "Beers count with ibu<60 from API does not match expected.");
    }

    /**
     * This test validates API filtering over hops values capability by comparing beer counts.
     */
    @Test(priority = 9)
    void testApiFilteringHops() {

        int page = 1;
        int size = MAX_PER_PAGE;
        int count_all = 0;

        // Retrieve filtered values from all beers.
        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersSpecificPage(page);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            for (int beer = 0; beer < size; beer++) {
                try {
                    String hops_names = json.getString("ingredients[" + beer + "].hops.name");
                    if (hops_names.contains(HOPS_FILTER)) {
                        count_all++;
                    }
                } catch (NullPointerException e) {
                    int id = json.getInt("id[" + beer + "]");
                    System.out.println("Beer id " + id + " has null ibu value");
                }
            }
            page++;
        }

        //Retrieve filtered values via API.
        page = 1;
        size = MAX_PER_PAGE;
        int count_filtered = 0;

        while (size == MAX_PER_PAGE) {
            Response res = RestRequests.getAllBeersFilterByOneParamStringTriggerSpecificPage(page, "hops", HOPS_FILTER);
            JsonPath json = new JsonPath(res.asString());
            size = json.getInt("size()");
            count_filtered += size;
            page++;
        }
        Assert.assertEquals(count_all, count_filtered, "Beers count with hops = " + HOPS_FILTER + " from API does not match expected.");
    }
}