package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Legacy-style report helper.
 *
 * Every dependency used here is resolved from the JAR files that are committed
 * under Web/WEB-INF/lib. There is no Maven or Gradle descriptor in this project.
 */
public class App {

    private static final String[] HEADERS = { "Region", "Units", "Checksum" };

    /** Writes a small BIFF8 workbook using Apache POI (poi-3.11-beta2.jar). */
    public static File writeReport(File target) throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Sales");

        HSSFRow header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            HSSFCell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }

        HSSFRow row = sheet.createRow(1);
        row.createCell(0).setCellValue("EMEA");
        row.createCell(1).setCellValue(1425);
        row.createCell(2).setCellValue(checksum("EMEA:1425"));

        FileOutputStream out = new FileOutputStream(target);
        try {
            workbook.write(out);
        } finally {
            out.close();
        }
        return target;
    }

    /** Reads the workbook back with JExcelAPI (jxl.jar). */
    public static String readFirstDataRow(File source) throws Exception {
        jxl.Workbook workbook = jxl.Workbook.getWorkbook(source);
        try {
            jxl.Sheet sheet = workbook.getSheet(0);
            List<String> values = new ArrayList<String>();
            for (int col = 0; col < sheet.getColumns(); col++) {
                values.add(sheet.getCell(col, 1).getContents());
            }
            return StringUtils.join(values.iterator(), " | ");
        } finally {
            workbook.close();
        }
    }

    /** Uses commons-codec for hashing and Base64 encoding. */
    public static String checksum(String value) {
        byte[] digest = DigestUtils.sha256(value);
        String encoded = Base64.encodeBase64String(digest);
        return StringUtils.substring(encoded, 0, 16);
    }

    /** Uses the pre-generics commons-collections 3.2.1 API. */
    @SuppressWarnings("unchecked")
    public static List<String> knownRegions() {
        List left = new ArrayList();
        left.add("EMEA");
        left.add("APAC");

        List right = new ArrayList();
        right.add("APAC");
        right.add("LATAM");

        List merged = new ArrayList(CollectionUtils.union(left, right));
        java.util.Collections.sort(merged);
        return merged;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("commons-lang   : " + StringUtils.repeat("-", 12));
        System.out.println("commons-codec  : " + checksum("EMEA:1425"));
        System.out.println("collections    : " + knownRegions());

        File report = File.createTempFile("legacy-report", ".xls");
        report.deleteOnExit();
        writeReport(report);
        System.out.println("poi wrote      : " + report.length() + " bytes");
        System.out.println("jxl read back  : " + readFirstDataRow(report));
    }
}
