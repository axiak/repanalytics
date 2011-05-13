package util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SimpleMD5 {
    public static String md5hex(String input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            return hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("This shoul dnever happen: MD5 doesn't exist.");
        }

    }
}
