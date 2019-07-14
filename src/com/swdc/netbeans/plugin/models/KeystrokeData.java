/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.models;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.OfflineManager;
import com.swdc.netbeans.plugin.managers.SessionManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class KeystrokeData {
    
    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private List<KeystrokeFileMetrics> sourceList = new ArrayList<>();
    private String version;
    private String os;
    private int pluginId;
    private int keystrokes = 0;
    // start and end are in seconds
    private long start;
    private long local_start;
    private String timezone;
    private KeystrokeProject project;
    
    private static final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();
    
    public KeystrokeData(String projectName, String projectDir) {
        this.version = softwareUtil.getVersion();
        this.os = softwareUtil.getOs();
        this.pluginId = SoftwareUtil.PLUGIN_ID;
        
        this.project = new KeystrokeProject(projectName, projectDir);
    }
    
    public void resetData() {
        this.sourceList = new ArrayList<>();
        
        this.start = 0L;
        this.local_start = 0L;
        this.timezone = "";
    }
    
    public KeystrokeMetrics getMetrics(String fileName) {

        for (KeystrokeFileMetrics fileMetrics : sourceList) {
            if (fileMetrics.getFileName().equals(fileName)) {
                // set the end time to files that are still unended
                endPreviousModifiedFiles(fileName);
                // return the file metrics
                return fileMetrics.getMetrics();
            }
        }

        SoftwareUtil.TimesData timesData = softwareUtil.getTimesData();
        
        if (this.start == 0) {
            this.start = timesData.now;
            this.local_start = timesData.local_now;
            this.timezone = timesData.timezone;
            
            // initialize the 5 minute timer to store the payload
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 60);
                    this.processKeystrokes();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
        }

        // not found, create one
        KeystrokeFileMetrics fileMetrics = new KeystrokeFileMetrics(fileName);
        fileMetrics.getMetrics().setStart(timesData.now);
        fileMetrics.getMetrics().setLocal_start(timesData.local_now);
        sourceList.add(fileMetrics);

        return fileMetrics.getMetrics();
    }
    
    public void processKeystrokes() {
        if (this.hasData()) {
            // set the end time to any files that haven't ended
            this.endUnendedFiles();
            
            String payload = this.getPayload();

            // update the keystrokes and minutes value
            OfflineManager.getInstance().incrementSessionSummaryData(1, keystrokes);

            // save the data offline
            softwareUtil.storePayload(payload);
        }

        this.resetData();
        
        SessionManager.fetchDailyKpmSessionInfo(false);
    }

    public void endPreviousModifiedFiles(String currentFileName) {
        SoftwareUtil.TimesData timesData = softwareUtil.getTimesData();
        for (KeystrokeFileMetrics fileMetrics : sourceList) {
            if (fileMetrics.getFileName().equals(currentFileName)) {
                fileMetrics.getMetrics().setEnd(0L);
                fileMetrics.getMetrics().setLocal_end(0L);
            } else if (fileMetrics.getMetrics().getEnd() == 0) {
                fileMetrics.getMetrics().setEnd(timesData.now);
                fileMetrics.getMetrics().setLocal_end(timesData.local_now);
            }
        }
    }

    public void endUnendedFiles() {
        SoftwareUtil.TimesData timesData = softwareUtil.getTimesData();
        for (KeystrokeFileMetrics fileMetrics : sourceList) {
            if (fileMetrics.getMetrics().getEnd() == 0) {
                fileMetrics.getMetrics().setEnd(timesData.now);
                fileMetrics.getMetrics().setLocal_end(timesData.local_now);
            }
        }
    }
    
    public String getPayload() {
        if (this.hasData()) {
            
            JsonObject jsonObj = new JsonObject();
            JsonObject sourceObj = new JsonObject();
            sourceList.forEach((fileMetrics) -> {
                JsonElement jsonEl = SoftwareUtil.gson.toJsonTree(fileMetrics.getMetrics());
                
                sourceObj.add(fileMetrics.getFileName(), jsonEl);
            });
            jsonObj.add("source", sourceObj);
            jsonObj.addProperty("version", this.version);
            jsonObj.addProperty("os", this.os);
            jsonObj.addProperty("pluginId", this.pluginId);
            jsonObj.addProperty("keystrokes", this.keystrokes);
            jsonObj.addProperty("start", this.start);
            jsonObj.addProperty("local_start", this.local_start);
            jsonObj.addProperty("timezone", this.timezone);
            jsonObj.add("project", SoftwareUtil.gson.toJsonTree(this.project));
            
            return jsonObj.toString();
        }
        return null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPluginId() {
        return pluginId;
    }

    public void setPluginId(int pluginId) {
        this.pluginId = pluginId;
    }

    public int getKeystrokes() {
        return keystrokes;
    }

    public void setKeystrokes(int keystrokes) {
        this.keystrokes = keystrokes;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getLocal_start() {
        return local_start;
    }

    public void setLocal_start(long local_start) {
        this.local_start = local_start;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public KeystrokeProject getProject() {
        return project;
    }

    public void setProject(KeystrokeProject project) {
        this.project = project;
    }

    public List<KeystrokeFileMetrics> getSourceList() {
        return sourceList;
    }

    public boolean hasData() {
        return this.keystrokes > 0;
    }
}
