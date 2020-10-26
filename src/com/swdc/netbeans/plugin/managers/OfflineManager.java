/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xavierluiz
 */
public class OfflineManager {
    public static final Logger LOG = Logger.getLogger("OfflineManager");

    private static OfflineManager instance = null;

    public SessionSummaryData sessionSummaryData = new SessionSummaryData();

    public static OfflineManager getInstance() {
        if (instance == null) {
            instance = new OfflineManager();
        }
        return instance;
    }

    protected class SessionSummaryData {
        public int currentDayMinutes;
        public int averageDailyMinutes;
        public int averageDailyKeystrokes;
        public int currentDayKeystrokes;
        public int liveshareMinutes;
    }

    public void setSessionSummaryData(int minutes, int keystrokes, int averageDailyMinutes) {
        sessionSummaryData = new SessionSummaryData();
        sessionSummaryData.currentDayKeystrokes = keystrokes;
        sessionSummaryData.currentDayMinutes = minutes;
        sessionSummaryData.averageDailyMinutes = averageDailyMinutes;
        saveSessionSummaryToDisk();
    }

    public void clearSessionSummaryData() {
        sessionSummaryData = new SessionSummaryData();
        saveSessionSummaryToDisk();
    }

    public void setSessionSummaryLiveshareMinutes(int minutes) {
        sessionSummaryData.liveshareMinutes = minutes;
    }

    public void incrementSessionSummaryData(int minutes, int keystrokes) {
        sessionSummaryData.currentDayMinutes += minutes;
        sessionSummaryData.currentDayKeystrokes += keystrokes;
        saveSessionSummaryToDisk();
    }

    public String getSessionSummaryFile() {
        String file = SoftwareUtil.getSoftwareDir(true);
        if (SoftwareUtil.isWindows()) {
            file += "\\sessionSummary.json";
        } else {
            file += "/sessionSummary.json";
        }
        return file;
    }

    public void updateStatusBarWithSummaryData(JsonObject sessionSummary) {

        int averageDailyMinutes = 0;
        if (sessionSummary.has("averageDailyMinutes")) {
            averageDailyMinutes = sessionSummary.get("averageDailyMinutes").getAsInt();
        }
        int currentDayMinutes = 0;
        if (sessionSummary.has("currentDayMinutes")) {
            currentDayMinutes = sessionSummary.get("currentDayMinutes").getAsInt();
        }

        String currentDayTimeStr = SoftwareUtil.humanizeMinutes(currentDayMinutes);
        String averageDailyMinutesTimeStr = SoftwareUtil.humanizeMinutes(averageDailyMinutes);

        SoftwareStatusBar.StatusBarType barType = currentDayMinutes > averageDailyMinutes
                ? SoftwareStatusBar.StatusBarType.ROCKET
                : SoftwareStatusBar.StatusBarType.NO_KPM;
        String msg = currentDayTimeStr;
        if (averageDailyMinutes > 0) {
            msg += " | " + averageDailyMinutesTimeStr;
        }

        SoftwareUtil.setStatusLineMessage(barType, msg,
                "Code time today vs. your daily average. Click to see more from Code Time");

        SoftwareUtil.fetchCodeTimeMetricsDashboard(sessionSummary);
    }

    public void saveSessionSummaryToDisk() {
        File f = new File(getSessionSummaryFile());

        final String summaryDataJson = SoftwareUtil.gson.toJson(sessionSummaryData);

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(summaryDataJson);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                /* ignore */}
        }
    }

    public JsonObject getSessionSummaryFileAsJson() {
        JsonObject data = null;

        String sessionSummaryFile = getSessionSummaryFile();
        File f = new File(sessionSummaryFile);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(sessionSummaryFile));
                String content = new String(encoded, Charset.defaultCharset());
                // json parse it
                data = SoftwareUtil.jsonParser.parse(content).getAsJsonObject();
            } catch (JsonSyntaxException | IOException e) {
                LOG.log(Level.SEVERE, "Code Time: Error trying to read and json parse the session file.", e);
            }
        } else {
            String jsonStr = SoftwareUtil.gson.toJson(new SessionSummaryData());
            data = (JsonObject) SoftwareUtil.jsonParser.parse(jsonStr);
        }
        return data;
    }

    public String getSessionSummaryInfoFileContent() {
        String content = null;

        String sessionSummaryFile = SoftwareUtil.getSummaryInfoFile(true);
        File f = new File(sessionSummaryFile);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(sessionSummaryFile));
                content = new String(encoded, Charset.defaultCharset());
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Code Time: Error trying to read and json parse the session file.", e);
            }
        }
        return content;
    }

    public void saveFileContent(String content, String file) {
        File f = new File(file);

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));
            writer.write(content);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                /* ignore */}
        }
    }
}
