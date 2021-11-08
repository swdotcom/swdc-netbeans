/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.software.codetime.SoftwareUtil;
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
import swdc.java.ops.manager.FileUtilManager;

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

    public void saveSessionSummaryToDisk() {
        File f = new File(FileUtilManager.getSessionDataSummaryFile());

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

        String sessionSummaryFile = FileUtilManager.getSessionDataSummaryFile();
        File f = new File(sessionSummaryFile);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(sessionSummaryFile));
                String content = new String(encoded, Charset.defaultCharset());
                // json parse it
                data = SoftwareUtil.readAsJsonObject(content);
            } catch (JsonSyntaxException | IOException e) {
                LOG.log(Level.SEVERE, "Code Time: Error trying to read and json parse the session file.", e);
            }
        } else {
            String jsonStr = SoftwareUtil.gson.toJson(new SessionSummaryData());
            data = SoftwareUtil.readAsJsonObject(jsonStr);
        }
        return data;
    }

    public String getSessionSummaryInfoFileContent() {
        String content = null;

        String sessionSummaryFile = FileUtilManager.getSessionDataSummaryFile();
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
