/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.SessionSummary;

public class SessionDataManager {

    public static void clearSessionSummaryData() {
        SessionSummary summary = new SessionSummary();
        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
    }
    
    public static SessionSummary getSessionSummaryFileData() {
    	try {
        	JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getSessionDataSummaryFile());
        	Type type = new TypeToken<SessionSummary>() {}.getType();
        	return UtilManager.gson.fromJson(jsonObj, type);
    	} catch (Exception e) {
    		return new SessionSummary();
    	}
    } 

    public static void updateSessionSummaryFromServer() {
        SessionSummary summary = SessionDataManager.getSessionSummaryFileData();

        String jwt = FileUtilManager.getItem("jwt");
        String api = "/sessions/summary";
        ClientResponse resp = OpsHttpClient.softwareGet(api, jwt);
        if (resp.isOk()) {
            try {
                Type type = new TypeToken<SessionSummary>() {}.getType();
                summary = UtilManager.gson.fromJson(resp.getJsonObj(), type);
            } catch (Exception e) {
                //
            }
        }

        updateFileSummaryAndStatsBar(summary);
    }
    
    public static void updateFileSummaryAndStatsBar(SessionSummary sessionSummary) {
        StatusBarManager.updateStatusBar(sessionSummary);
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
