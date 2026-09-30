package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Legacy spreadsheet report helper.
 *
 * Every library used here comes from a JAR committed under Web/WEB-INF/lib.
 * This project has no pom.xml and no Gradle build script: the JAR files are the
 * only representation of its dependencies.
 */
public class App {

    private static final String[] HEADERS = { "Region", "Units", "Checksum" };

    /** Apache POI (poi-3.11-beta2.jar): write a BIFF8 .xls workbook. */
    public static File writeLegacyReport(File target) throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Sales");

        HSSFRow header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
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

    /**
     * Apache POI OOXML (poi-ooxml-3.11-beta2.jar): build an .xlsx workbook.
     *
     * Compiled against the committed poi-ooxml JAR. It is not called from
     * main() because POI's OOXML support additionally needs xmlbeans and
     * poi-ooxml-schemas at runtime, and those JARs are deliberately not
     * committed here so the fixture stays at exactly six dependencies.
     */
    public static XSSFSheet buildOoxmlSheet() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Sales");
        sheet.createRow(0).createCell(0).setCellValue(HEADERS[0]);
        return sheet;
    }

    /** JExcelAPI (jxl.jar): read the .xls workbook back. */
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

    /** Commons Codec (commons-codec-1.12.jar) plus Commons Lang for trimming. */
    public static String checksum(String value) {
        byte[] digest = DigestUtils.sha256(value);
        return StringUtils.substring(Base64.encodeBase64String(digest), 0, 16);
    }

    /** Commons Collections (commons-collections-3.2.1.jar), pre-generics API. */
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
        System.out.println("commons-lang  : " + StringUtils.repeat("-", 12));
        System.out.println("commons-codec : " + checksum("EMEA:1425"));
        System.out.println("collections   : " + knownRegions());

        File report = File.createTempFile("legacy-report", ".xls");
        report.deleteOnExit();
        writeLegacyReport(report);
        System.out.println("poi wrote     : " + report.length() + " bytes");
        System.out.println("jxl read back : " + readFirstDataRow(report));
    }
}
