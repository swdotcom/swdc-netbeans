/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
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
        String api = "/sessions?summary=true";
        SoftwareResponse softwareResponse = softwareUtil.makeApiCall(api, HttpGet.METHOD_NAME, null);
        JsonObject jsonObj = softwareResponse.getJsonObj();
        if (jsonObj != null) {
            boolean inFlow = true;
            if (jsonObj.has("inFlow")) {
                inFlow = jsonObj.get("inFlow").getAsBoolean();
            }
//            if (jsonObj.has("currentSessionGoalPercent")) {
//                float currentSessionGoalPercent = jsonObj.get("currentSessionGoalPercent").getAsFloat();
//                if (currentSessionGoalPercent > 0) {
//                    if (currentSessionGoalPercent < 0.40) {
//                        barType = StatusBarType.QUARTER;
//                    } else if (currentSessionGoalPercent < 0.70) {
//                        barType = StatusBarType.HALF;
//                    } else if (currentSessionGoalPercent < 0.93) {
//                        barType = StatusBarType.ALMOST;
//                    } else if (currentSessionGoalPercent < 1.3) {
//                        barType = StatusBarType.FULL;
//                    }
//                }
//            }
            int lastKpm = 0;
            if (jsonObj.has("lastKpm")) {
                lastKpm = jsonObj.get("lastKpm").getAsInt();
            }
            int currentSessionMinutes = 0;
            if (jsonObj.has("currentSessionMinutes")) {
                currentSessionMinutes = jsonObj.get("currentSessionMinutes").getAsInt();
            }
            int averageDailyMinutes = 0;
            if (jsonObj.has("averageDailyMinutes")) {
                averageDailyMinutes = jsonObj.get("averageDailyMinutes").getAsInt();
            }
            int currentDayMinutes = 0;
            if (jsonObj.has("currentDayMinutes")) {
                currentDayMinutes = jsonObj.get("currentDayMinutes").getAsInt();
            }
            
            String sessionTimeStr = softwareUtil.humanizeMinutes(currentSessionMinutes);
            String currentDayTimeStr = softwareUtil.humanizeMinutes(currentDayMinutes);
            String averageDailyMinutesTimeStr = softwareUtil.humanizeMinutes(averageDailyMinutes);

            StatusBarType barType = currentDayMinutes > averageDailyMinutes ? StatusBarType.ROCKET : StatusBarType.NO_KPM;
            String msg = "Code time today: " + currentDayTimeStr;
            if (averageDailyMinutes > 0) {
                msg += " | Avg: " + averageDailyMinutesTimeStr;
            }
            
            softwareUtil.setStatusLineMessage(barType, msg, "Click to see more from Code Time");

        } else if (!softwareResponse.isDeactivated()) {
            softwareUtil.setStatusLineMessage(StatusBarType.ALERT, "Code Time", "Click to log in to Code Time");
            LOG.log(Level.WARNING, "Code Time", "Click to log in to Code Time");
            softwareUtil.checkUserAuthenticationStatus();
        } else {
            // check in a day
            LOG.log(Level.WARNING, "Code Time", "Click to log in to Code Time");
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 60 * 60 * 24);
                    softwareUtil.checkUserAuthenticationStatus();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
        }
    }
}
