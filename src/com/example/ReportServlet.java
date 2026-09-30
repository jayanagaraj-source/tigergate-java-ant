package com.example;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Legacy reporting endpoint.
 *
 * Uses the servlet API, Log4j, Commons IO and Jackson - all of them resolved
 * from JAR files committed under Web/WEB-INF/lib, with no Maven or Gradle
 * descriptor anywhere in this project.
 */
public class ReportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(ReportServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String region = request.getParameter("region");
        LOG.info("Building report for region=" + region);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("regions", App.knownRegions());
        payload.put("checksum", App.checksum("EMEA:1425"));

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(payload));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        byte[] uploaded = IOUtils.toByteArray(request.getInputStream());
        LOG.info("Received " + uploaded.length + " bytes");

        try {
            File report = File.createTempFile("legacy-report", ".xls");
            report.deleteOnExit();
            App.writeLegacyReport(report);
            response.setContentType("application/vnd.ms-excel");
            response.getOutputStream().write(IOUtils.toByteArray(new java.io.FileInputStream(report)));
        } catch (Exception e) {
            LOG.error("Report generation failed", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String toJson(Map<String, Object> payload) throws IOException {
        return new ObjectMapper().writeValueAsString(payload);
    }
}
