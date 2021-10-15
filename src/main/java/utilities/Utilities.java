package utilities;

import java.util.List;

/**
 * This class contains reusable general purpose utilities functions.
 */
public class Utilities {

    /**
     * Counts average from given list of values.
     *
     * @param list given list of values to perform calculations on
     * @return counted average
     */
    public static Double countAverageFromList(List<Double> list) {

        double sum = 0;
        for (double num : list) {
            sum += num;
        }
        return sum / list.size();
    }
}
