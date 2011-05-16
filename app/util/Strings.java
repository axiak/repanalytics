package util;


import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Strings {
    private final static Pattern PUNCTUATION_RE = Pattern.compile("[.,();!@#$%^&*\\[\\]:'\"{}\\\\|_/+-]+");
    private final static Pattern SPACE_RE = Pattern.compile("[\\n\\r\\s]+", Pattern.DOTALL);

    public static String normalizeSimple(String input) {
        if (input == null) {
            return "";
        } else {
            return input.trim().toLowerCase(Locale.getDefault());
        }
    }

    public static String normalizeNoPunctuation(String input) {
        String value = normalizeSimple(input);
        return SPACE_RE.matcher(PUNCTUATION_RE.matcher(value).replaceAll(" ")).replaceAll(" ").trim();
    }

    public static String normalizePhone(String input) {
        if (input == null) {
            return "";
        }
        try {
            PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
            return pnu.format(pnu.parse(input, "US"), PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            return normalizeNoPunctuation(input);
        }
    }

    public static int getLevenshteinDistance (String s, String t) {
        checkNotNull(s); checkNotNull(t);

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        int p[] = new int[n+1]; //'previous' cost array, horizontally
        int d[] = new int[n+1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i<=n; i++) {
            p[i] = i;
        }

        for (j = 1; j<=m; j++) {
            t_j = t.charAt(j-1);
            d[0] = j;

            for (i=1; i<=n; i++) {
                cost = s.charAt(i-1)==t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i-1]+1, p[i]+1),  p[i-1]+cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }
}
