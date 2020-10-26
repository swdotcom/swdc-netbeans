/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.OfflineManager.SessionSummaryData;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar.StatusBarType;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;

/**
 *
 */
public class SessionManager {
    
    public static final Logger LOG = Logger.getLogger("SessionManager");
    
    private static final OfflineManager offlineMgr = OfflineManager.getInstance();
    
    public static void fetchDailyKpmSessionInfo() {
        
        String sessionsApi = "/sessions/summary";

        // make an async call to get the kpm info
        JsonObject sessionSummary = SoftwareUtil.makeApiCall(sessionsApi, HttpGet.METHOD_NAME, null).getJsonObj();
        int currentDayMinutes = 0;
        if (sessionSummary != null) {

            if (sessionSummary.has("currentDayMinutes")) {
                currentDayMinutes = sessionSummary.get("currentDayMinutes").getAsInt();
            }
            int currentDayKeystrokes = 0;
            if (sessionSummary.has("currentDayKeystrokes")) {
                currentDayKeystrokes = sessionSummary.get("currentDayKeystrokes").getAsInt();
            }

            int averageDailyMinutes = 0;
            if (sessionSummary.has("averageDailyMinutes")) {
                averageDailyMinutes = sessionSummary.get("averageDailyMinutes").getAsInt();
            }

            offlineMgr.setSessionSummaryData(currentDayMinutes, currentDayKeystrokes, averageDailyMinutes);

        } else {
            SoftwareUtil.setStatusLineMessage(StatusBarType.NO_KPM, "Code Time", "Click to see more from Code Time");
            sessionSummary = offlineMgr.getSessionSummaryFileAsJson();
        }

        offlineMgr.updateStatusBarWithSummaryData(sessionSummary);
    }
    
}
