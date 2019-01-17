/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar.StatusBarType;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpGet;

/**
 *
 */
public class SessionManager {
    
    public static final Logger LOG = Logger.getLogger("SessionManager");
    
    private static final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();
    
    public static void fetchDailyKpmSessionInfo() {
        // set the start of the day
        Date d = softwareUtil.atStartOfDay(new Date());
        long fromSeconds = Math.round(d.getTime() / 1000);
        // make an async call to get the kpm 
        String api = "/sessions?from=" + fromSeconds + "&summary=true";
        SoftwareResponse softwareResponse = softwareUtil.makeApiCall(api, HttpGet.METHOD_NAME, null);
        JsonObject jsonObj = softwareResponse.getJsonObj();
        StatusBarType barType = StatusBarType.NO_KPM;
        if (jsonObj != null) {
            boolean inFlow = true;
            if (jsonObj.has("inFlow")) {
                inFlow = jsonObj.get("inFlow").getAsBoolean();
            }
            if (jsonObj.has("currentSessionGoalPercent")) {
                float currentSessionGoalPercent = jsonObj.get("currentSessionGoalPercent").getAsFloat();
                if (currentSessionGoalPercent > 0) {
                    if (currentSessionGoalPercent < 0.40) {
                        barType = StatusBarType.QUARTER;
                    } else if (currentSessionGoalPercent < 0.70) {
                        barType = StatusBarType.HALF;
                    } else if (currentSessionGoalPercent < 0.93) {
                        barType = StatusBarType.ALMOST;
                    } else if (currentSessionGoalPercent < 1.3) {
                        barType = StatusBarType.FULL;
                    }
                }
            }
            int lastKpm = 0;
            if (jsonObj.has("lastKpm")) {
                lastKpm = jsonObj.get("lastKpm").getAsInt();
            }
            long currentSessionMinutes = 0;
            if (jsonObj.has("currentSessionMinutes")) {
                currentSessionMinutes = jsonObj.get("currentSessionMinutes").getAsLong();
            }
            String sessionTime = "";
            if (currentSessionMinutes == 60) {
                sessionTime = "1 hr";
            } else if (currentSessionMinutes > 60) {
                float fval = (float)currentSessionMinutes / 60;
                try {
                    sessionTime = String.format("%.2f", fval) + " hrs";
                } catch (Exception e) {
                    sessionTime = String.valueOf(fval);
                }
            } else if (currentSessionMinutes == 1) {
                sessionTime = "1 min";
            } else {
                sessionTime = currentSessionMinutes + " min";
            }
            if (lastKpm > 0 || currentSessionMinutes > 0) {
                String sessionMsg = (sessionTime.equals("")) ? sessionTime : sessionTime;
                String statusMsg = String.valueOf(lastKpm) + " KPM, " + sessionMsg;
                if (inFlow) {
                    barType = StatusBarType.ROCKET;
                    // statusMsg = "🚀" + " " + statusMsg;
                }
                softwareUtil.setStatusLineMessage(barType,
                        "<S> " + statusMsg,
                        "Click to see more from Software.com");
            } else {
                softwareUtil.setStatusLineMessage(barType,
                        "Software.com",
                        "Click to see more from Software.com");
            }
        } else {
            softwareUtil.setStatusLineMessage(StatusBarType.ALERT, "Software.com", "Click to log in to Software.com");
            LOG.log(Level.WARNING, "Software.com", "Click to log in to Software.com");
            softwareUtil.checkUserAuthenticationStatus();
        }
    }
}
