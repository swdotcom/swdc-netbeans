/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.apple.eawt.Application;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.SessionSummary;
import java.lang.reflect.Type;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.apache.http.client.methods.HttpGet;

public class WallClockManager {

    public static final Logger log = Logger.getLogger("WallClockManager");

    private static final int SECONDS_INCREMENT = 30;
    private static final int DAY_CHECK_TIMER_INTERVAL = 60;

    private static WallClockManager instance = null;
    private AsyncManager asyncManager;
    private static boolean dispatching = false;

    public static WallClockManager getInstance() {
        if (instance == null) {
            synchronized (log) {
                if (instance == null) {
                    instance = new WallClockManager();
                }
            }
        }
        return instance;
    }

    private WallClockManager() {
        asyncManager = AsyncManager.getInstance();
        // initialize the timer
        this.init();
    }

    private void init() {
        final Runnable wallClockTimer = () -> updateWallClockTime();
        asyncManager.scheduleService(
                wallClockTimer, "wallClockTimer", 0, SECONDS_INCREMENT);

        final Runnable newDayCheckerTimer = () -> newDayChecker();
        asyncManager.scheduleService(
                newDayCheckerTimer, "newDayCheckerTimer", 30, DAY_CHECK_TIMER_INTERVAL);

        dispatchStatusViewUpdate();
    }

    public void newDayChecker() {
        if (SoftwareUtil.isNewDay()) {
            // send the payloads
            FileManager.sendOfflineData();

            // clear the last payload we have in memory
            FileManager.clearLastSavedKeystrokeStats();

            // send the time data
            TimeDataManager.sendOfflineTimeData();

            // clear the wc time and the session summary and the file change info summary
            clearWcTime();
            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            String day = SoftwareUtil.getTodayInStandardFormat();
            FileManager.setItem("currentDay", day);

            // update the last payload timestamp
            FileManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

            final Runnable service = () -> updateSessionSummaryFromServer();
            AsyncManager.getInstance().executeOnceInSeconds(service, 60);

        }
    }

    public void updateSessionSummaryFromServer() {
        SessionSummary summary = SessionDataManager.getSessionSummaryData();

        String jwt = FileManager.getItem("jwt");
        String api = "/sessions/summary?refresh=true";
        SoftwareResponse resp = SoftwareUtil.makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
        if (resp.isOk()) {
            JsonObject jsonObj = resp.getJsonObj();

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
            SessionSummary fetchedSummary = SoftwareUtil.gson.fromJson(jsonObj, type);

            // clone all
            summary.clone(fetchedSummary);

            TimeDataManager.updateSessionFromSummaryApi(fetchedSummary.getCurrentDayMinutes());

            // save the file
            FileManager.writeData(SessionDataManager.getSessionDataSummaryFile(), summary);
        }
    }

    private void updateWallClockTime() {
        // pass control from a background thread to the event dispatch thread,
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean isActive = true; // TODO: figure out how to get focus state
                if (isActive && SoftwareEventManager.isCurrentlyActive) {
                    long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
                    FileManager.setNumericItem("wctime", wctime);

                    // update the json time data file
                    TimeDataManager.incrementEditorSeconds(SECONDS_INCREMENT);
                }
                dispatchStatusViewUpdate();
            }
        });
    }

    public synchronized void dispatchStatusViewUpdate() {
        if (!dispatching) {
            dispatching = true;

            CodeTimeSummary ctSummary = TimeDataManager.getCodeTimeSummary();

            String icon = SoftwareUtil.showingStatusText() ? "paw-grey.png" : "clock.png";
            String currentDayTimeStr = SoftwareUtil.humanizeMinutes(ctSummary.activeCodeTimeMinutes);

            // STATUS BAR REFRESH
            SoftwareUtil.updateStatusBar(
                icon, currentDayTimeStr, "Code time today. Click to see more from Code Time.");

            // TREE REFRESH
            CodeTimeTreeTopComponent.refreshTree();
        }
        dispatching = false;
    }

    private void clearWcTime() {
        setWcTime(0);
    }

    public long getWcTimeInSeconds() {
        return FileManager.getNumericItem("wctime", 0L);
    }

    public void setWcTime(long seconds) {
        FileManager.setNumericItem("wctime", seconds);
        updateWallClockTime();
    }

}
