/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.models.KeystrokeAggregate;
import java.lang.reflect.Type;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.SessionSummary;

public class SessionDataManager {

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static SessionSummary getSessionSummaryData() {
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        if (jsonObj == null) {
            clearSessionSummaryData();
            jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        }
        JsonElement lastUpdatedToday = jsonObj.get("lastUpdatedToday");
        if (lastUpdatedToday != null) {
            // make sure it's a boolean and not a number
            if (!lastUpdatedToday.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = lastUpdatedToday.getAsInt() != 0;
                jsonObj.addProperty("lastUpdatedToday", newVal);
            }
        }
        JsonElement inFlow = jsonObj.get("inFlow");
        if (inFlow != null) {
            // make sure it's a boolean and not a number
            if (!inFlow.getAsJsonPrimitive().isBoolean()) {
                // set it to boolean
                boolean newVal = inFlow.getAsInt() != 0;
                jsonObj.addProperty("inFlow", newVal);
            }
        }
        Type type = new TypeToken<SessionSummary>() {}.getType();
        SessionSummary summary = SoftwareUtil.gson.fromJson(jsonObj, type);
        return summary;
    }

    public static void incrementSessionSummary(KeystrokeAggregate aggregate, long sessionSeconds) {
        SessionSummary summary = getSessionSummaryData();

        long sessionMinutes = sessionSeconds / 60;
        summary.setCurrentDayMinutes(summary.getCurrentDayMinutes() + sessionMinutes);

        summary.setCurrentDayKeystrokes(summary.getCurrentDayKeystrokes() + aggregate.keystrokes);
        summary.setCurrentDayLinesAdded(summary.getCurrentDayLinesAdded() + aggregate.linesAdded);
        summary.setCurrentDayLinesRemoved(summary.getCurrentDayLinesRemoved() + aggregate.linesRemoved);

        // save the file
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }

    public static ElapsedTime getTimeBetweenLastPayload() {
        ElapsedTime eTime = new ElapsedTime();

        // default of 1 minute
        long sessionSeconds = 60;
        long elapsedSeconds = 60;

        long lastPayloadEnd = FileUtilManager.getNumericItem("latestPayloadTimestampEndUtc", 0);
        if (lastPayloadEnd > 0) {
            UtilManager.TimesData timesData = UtilManager.getTimesData();
            elapsedSeconds = Math.max(60, timesData.now - lastPayloadEnd);
            long sessionThresholdSeconds = 60 * 15;
            if (elapsedSeconds > 0 && elapsedSeconds <= sessionThresholdSeconds) {
                sessionSeconds = elapsedSeconds;
            }
            sessionSeconds = Math.max(60, sessionSeconds);
        }

        eTime.sessionSeconds = sessionSeconds;
        eTime.elapsedSeconds = elapsedSeconds;

        return eTime;
    }
}
