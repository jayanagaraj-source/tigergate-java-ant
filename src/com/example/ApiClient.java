package com.example;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

/**
 * INTENTIONALLY VULNERABLE - SAST and secret-scanner fixture.
 * Do not copy any of this.
 *
 * Planted issues: API tokens and passwords hardcoded in source, credentials
 * embedded in a URL, cleartext HTTP for an authenticated call, and a token
 * written to stdout.
 */
public class ApiClient {

    // Hardcoded third-party credentials (fabricated - these authenticate to nothing).
    private static final String GITHUB_TOKEN = "ghp_9kQm2WvT7xLpN4dZaR8sYfB3cJ6hE1uG5oKi";
    private static final String SLACK_TOKEN = "xoxb-2947103857264-3847561092837-Kq7mZpX2vLnR8dTwYbF4jH6c";
    private static final String STRIPE_KEY =
            "sk_test_51MqR7xKvLpN2dZaB8sYfC3jH6uG9wEtQm4XoPrZiV7kNdT2yWbF5cJ8hA1sRu";

    private static final String BASIC_USER = "reporting-bot";
    private static final String BASIC_PASSWORD = "R3port!ngB0t2018";

    // Credentials embedded directly in the endpoint URL.
    private static final String LEGACY_ENDPOINT =
            "http://reporting-bot:R3port!ngB0t2018@api.internal.example/v1/upload";

    private static final String SESSION_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiJzdmNfcmVwb3J0aW5nIiwibmFtZSI6IlJlcG9ydGluZyBCb3QiLCJpYXQiOjE1MTYyMzkwMjJ9."
            + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    /** Sends the hardcoded bearer token over cleartext HTTP. */
    public int pushReport(byte[] payload) throws Exception {
        URL url = new URL("http://api.internal.example/v1/reports");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + GITHUB_TOKEN);
        conn.setRequestProperty("X-Slack-Token", SLACK_TOKEN);
        conn.setDoOutput(true);
        conn.getOutputStream().write(payload);
        return conn.getResponseCode();
    }

    /** Builds a Basic auth header from hardcoded credentials and logs it. */
    public String basicAuthHeader() throws Exception {
        String raw = BASIC_USER + ":" + BASIC_PASSWORD;
        String encoded = Base64.encodeBase64String(raw.getBytes("UTF-8"));
        System.out.println("Using Authorization: Basic " + encoded);
        return "Basic " + encoded;
    }

    public InputStream fetchLegacy() throws Exception {
        System.out.println("Calling " + LEGACY_ENDPOINT + " with jwt " + SESSION_JWT);
        return new URL(LEGACY_ENDPOINT).openStream();
    }

    public String describe() {
        return StringUtils.abbreviate("stripe=" + STRIPE_KEY, 24);
    }
}
