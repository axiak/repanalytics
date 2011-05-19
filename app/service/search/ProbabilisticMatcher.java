package service.search;

import com.google.common.collect.ImmutableMap;
import models.businesses.Business;
import play.Logger;
import util.Strings;

import java.util.Map;

import static util.Strings.normalizeNoPunctuation;
import static util.Strings.normalizePhone;

public final class ProbabilisticMatcher extends AbstractBusinessMatcher {
    Map<String, Double> weights = ImmutableMap.of(
            "name", 0.9,
            "phone", 1.0,
            "address", 0.7
    );
    double defaultWeight = 0.52;

    public ProbabilisticMatcher(Map<String, String> searchParameters) {
        super(searchParameters);
    }

    @Override
    public double getMatchScore(Business business, Map<String, String> parameters) {
        double total = 1;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            double weight = weights.containsKey(entry.getKey()) ? weights.get(entry.getKey()) : defaultWeight;
            total *= (1 - weight * compareField(entry.getKey(), entry.getValue(), business));
        }
        return 1 - total;
    }

    /*
     * The number returned here should be between 0 and 1 inclusive.
     */
    private double compareField(String key, String value, Business business) {
        String bValue = getBusinessField(business, key);

        if ("phone".equals(key)) {
            return normalizePhone(bValue).equals(normalizePhone(value)) ? 1 : 0;
        }

        bValue = normalizeNoPunctuation(bValue);
        value = normalizeNoPunctuation(value);

        if (bValue.equals(value)) {
            return 1;
        }

        int denominator = bValue.length() > value.length() ? bValue.length() : value.length();
        // TODO - Use tf-idf rather than levenshtein? (with stopwords, etc)
        return (1 - ((double) Strings.getLevenshteinDistance(value, bValue)) / denominator);
    }
}
