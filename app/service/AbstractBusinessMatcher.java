package service;

import com.google.common.base.Function;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import models.businesses.Business;
import play.Logger;
import play.libs.F;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

abstract public class AbstractBusinessMatcher implements Function<Business, F.Tuple<Double, Business>> {
    private Map<String, String> searchParameters;

    protected AbstractBusinessMatcher(Map<String, String> searchParameters) {
        this.searchParameters = searchParameters;
    }

    abstract public double getMatchScore(Business business, Map<String, String> parameters);

    @Override
    public F.Tuple<Double, Business> apply(@Nullable Business business) {
        return new F.Tuple<Double, Business>(getMatchScore(business, searchParameters), business);
    }

    protected final String getBusinessField(Business business, String parameter) {
        try {
            Field field = Business.class.getField(parameter);
            return (String)field.get(business);
        } catch (NoSuchFieldException e) {
            Logger.info(e, "Issue getting parameter from Business object.");
        } catch (IllegalAccessException e) {
            Logger.info(e, "Issue getting parameter from Business object.");
        }
        return "";
    }

    protected final String normalizeString(String input) {
        if (input == null) {
            return "";
        } else {
            return input.trim().toLowerCase(Locale.getDefault());
        }
    }

    protected final String normalizePhone(String phone) {
        try {
            PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
            return pnu.format(pnu.parse(phone, "US"), PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            return normalizeString(phone);
        }
    }
}
