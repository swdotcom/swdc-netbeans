/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.models;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 */
public class KeystrokeData {
    
    // TODO: backend driven, we should look at getting a list of types at some point
    private String type = "Events";

    // non-hardcoded attributes
    private List<KeystrokeFileMetrics> sourceList = new ArrayList<>();
    private String version;
    private int pluginId;
    private int keystrokes = 0;
    // start and end are in seconds
    private long start;
    private long local_start;
    private String timezone;
    private KeystrokeProject project;
    
    private static final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();
    
    public KeystrokeData(String projectName, String projectDir) {
        this.start = Math.round(System.currentTimeMillis() / 1000);
        this.version = softwareUtil.getPluginVersion();
        this.pluginId = SoftwareUtil.pluginId;
        
        // offset is negative if the tz is before utc, and positive if after
        Integer offset  = ZonedDateTime.now().getOffset().getTotalSeconds();
        this.local_start = this.start + offset;
        this.timezone = TimeZone.getDefault().getID();
        
        this.project = new KeystrokeProject(projectName, projectDir);
    }
    
    public void resetData() {
        this.start = Math.round(System.currentTimeMillis() / 1000);
        this.sourceList = new ArrayList<>();
        
        // offset is negative if the tz is before utc, and positive if after
        Integer offset  = ZonedDateTime.now().getOffset().getTotalSeconds();
        this.local_start = this.start + offset;
        this.timezone = TimeZone.getDefault().getID();
    }
    
    public KeystrokeMetrics getMetrics(String fileName) {

        for (KeystrokeFileMetrics fileMetrics : sourceList) {
            if (fileMetrics.getFileName().equals(fileName)) {
                return fileMetrics.getMetrics();
            }
        }
        
        // not found, create one
        KeystrokeFileMetrics fileMetrics = new KeystrokeFileMetrics(fileName);
        sourceList.add(fileMetrics);
        
        return fileMetrics.getMetrics();
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
