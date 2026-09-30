package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * INTENTIONALLY VULNERABLE - SAST fixture. Do not copy any of this.
 *
 * Planted issues: hardcoded database credentials, SQL injection via string
 * concatenation, and a credential logged in cleartext.
 */
public class VulnerableDao {

    // Hardcoded credentials (fabricated - these point at nothing).
    private static final String DB_URL = "jdbc:mysql://legacy-db.internal:3306/sales";
    private static final String DB_USER = "sa_reporting";
    private static final String DB_PASSWORD = "Pr0d!Reporting2019";

    private static final String CONNECTION_STRING =
            "jdbc:postgresql://reports:H4rdc0dedPgPass@db.internal:5432/warehouse";

    private Connection connect() throws Exception {
        System.out.println("Connecting to " + DB_URL + " as " + DB_USER + "/" + DB_PASSWORD);
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /** SQL injection: the region value is concatenated straight into the query. */
    public List<String> findUnitsByRegion(String region) throws Exception {
        List<String> results = new ArrayList<String>();
        Connection conn = connect();
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT units FROM sales WHERE region = '" + region + "'";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                results.add(rs.getString("units"));
            }
            rs.close();
            stmt.close();
        } finally {
            conn.close();
        }
        return results;
    }

    /** SQL injection in an UPDATE, reached through executeUpdate. */
    public int renameRegion(String oldName, String newName) throws Exception {
        Connection conn = DriverManager.getConnection(CONNECTION_STRING);
        try {
            Statement stmt = conn.createStatement();
            return stmt.executeUpdate(
                    "UPDATE sales SET region = '" + newName + "' WHERE region = '" + oldName + "'");
        } finally {
            conn.close();
        }
    }
}
