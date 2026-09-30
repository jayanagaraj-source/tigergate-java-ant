package com.example;

import java.security.MessageDigest;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

/**
 * INTENTIONALLY VULNERABLE - SAST fixture. Do not copy any of this.
 *
 * Planted issues: MD5 and SHA-1 for password hashing, DES in ECB mode, a
 * hardcoded symmetric key, and java.util.Random used to mint session tokens.
 */
public class InsecureCrypto {

    // Hardcoded symmetric key (fabricated).
    private static final byte[] DES_KEY = "l3g4cyK3".getBytes();

    private static final String SIGNING_SECRET = "s3cr3t-hmac-signing-key-do-not-rotate";

    /** Broken password hashing: MD5, unsalted. */
    public static String hashPassword(String password) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        return new String(Hex.encodeHex(md5.digest(password.getBytes("UTF-8"))));
    }

    /** Also broken: SHA-1, unsalted. */
    public static String legacyFingerprint(String value) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return new String(Hex.encodeHex(sha1.digest(value.getBytes("UTF-8"))));
    }

    /** DES in ECB mode with a hardcoded key. */
    public static byte[] encryptToken(String plaintext) throws Exception {
        SecretKeySpec key = new SecretKeySpec(DES_KEY, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext.getBytes("UTF-8"));
    }

    /** AES, but still ECB and still a hardcoded key. */
    public static byte[] encryptRecord(byte[] plaintext) throws Exception {
        SecretKeySpec key = new SecretKeySpec("0123456789abcdef".getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext);
    }

    /** Predictable session token: java.util.Random is not a CSPRNG. */
    public static String newSessionToken() {
        Random random = new Random(System.currentTimeMillis());
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            token.append(Integer.toHexString(random.nextInt()));
        }
        return token.toString();
    }

    public static String signingSecret() {
        return SIGNING_SECRET;
    }
}
