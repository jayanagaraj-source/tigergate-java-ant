package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * INTENTIONALLY VULNERABLE - SAST fixture. Do not copy any of this.
 *
 * Planted issues: OS command injection, path traversal, XXE, unsafe Java
 * deserialization, TLS verification disabled, and world-writable temp output.
 */
public class UnsafeIO {

    private static final File UPLOAD_ROOT = new File("/var/legacy/uploads");

    /** Command injection: the filename is interpolated into a shell command. */
    public static void convertUpload(String filename) throws Exception {
        String command = "/usr/bin/xls2csv " + filename + " > /var/legacy/out.csv";
        Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
        process.waitFor();
    }

    /** Command injection through the single-string exec overload. */
    public static void archiveRegion(String region) throws Exception {
        Runtime.getRuntime().exec("tar czf /var/legacy/" + region + ".tgz /var/legacy/uploads");
    }

    /** Path traversal: the caller-supplied path is not confined to UPLOAD_ROOT. */
    public static byte[] readUpload(String relativePath) throws Exception {
        File target = new File(UPLOAD_ROOT, relativePath);
        FileInputStream in = new FileInputStream(target);
        try {
            byte[] buffer = new byte[(int) target.length()];
            in.read(buffer);
            return buffer;
        } finally {
            in.close();
        }
    }

    /** XXE: external entities and DTDs are left enabled. */
    public static Document parseConfig(InputStream xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xml);
    }

    /**
     * Unsafe deserialization of untrusted input. This is especially dangerous
     * in this project because commons-collections 3.2.1 is on the classpath and
     * supplies a well-known gadget chain.
     */
    public static Object loadSession(InputStream untrusted) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(untrusted);
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }

    /** TLS certificate and hostname verification disabled. */
    public static void trustEverything() throws Exception {
        TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // accepts any client certificate
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // accepts any server certificate
            }
        } };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAll, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    /** Predictable temp file path in a world-writable directory. */
    public static File stageExport(byte[] payload) throws Exception {
        File staged = new File("/tmp/legacy-export.xls");
        FileOutputStream out = new FileOutputStream(staged);
        try {
            out.write(payload);
        } finally {
            out.close();
        }
        staged.setReadable(true, false);
        staged.setWritable(true, false);
        return staged;
    }
}
