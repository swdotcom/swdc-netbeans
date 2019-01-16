/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
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
        if (jsonObj != null) {
            boolean inFlow = true;
            if (jsonObj.has("inFlow")) {
                inFlow = jsonObj.get("inFlow").getAsBoolean();
            }
            String sessionTimeIcon = "";
            if (jsonObj.has("currentSessionGoalPercent")) {
                float currentSessionGoalPercent = jsonObj.get("currentSessionGoalPercent").getAsFloat();
                if (currentSessionGoalPercent > 0) {
                    if (currentSessionGoalPercent < 0.40) {
                        sessionTimeIcon = "🌘";
                    } else if (currentSessionGoalPercent < 0.70) {
                        sessionTimeIcon = "🌗";
                    } else if (currentSessionGoalPercent < 0.93) {
                        sessionTimeIcon = "🌖";
                    } else if (currentSessionGoalPercent < 1.3) {
                        sessionTimeIcon = "🌕";
                    } else {
                        sessionTimeIcon = "🌔";
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
                String sessionMsg = (sessionTime.equals("")) ? sessionTime : sessionTimeIcon + " " + sessionTime;
                String statusMsg = String.valueOf(lastKpm) + " KPM, " + sessionMsg;
                if (inFlow) {
                    statusMsg = "🚀" + " " + statusMsg;
                }
                softwareUtil.setStatusLineMessage("<S> " + statusMsg,
                    "Click to see more from Software.com");
            } else {
                softwareUtil.setStatusLineMessage("Software.com", "Click to see more from Software.com");
            }
        } else {
            LOG.log(Level.WARNING, "⚠️Software.com", "Click to log in to Software.com");
            softwareUtil.checkUserAuthenticationStatus();
        }
    }
}
