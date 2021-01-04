/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.netbeans.plugin.SoftwareUtil;
import static com.swdc.netbeans.plugin.managers.SoftwareSessionManager.LOG;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.model.SessionSummary;

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

        dispatchStatusViewUpdate(false);
    }

    public void newDayChecker() {
        if (SoftwareUtil.isNewDay()) {

            // clear the wc time and the session summary and the file change info summary
            clearWcTime();
            
            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            String day = SoftwareUtil.getTodayInStandardFormat();
            FileUtilManager.setItem("currentDay", day);

            // update the last payload timestamp
            FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", 0);

            final Runnable service = () -> updateSessionSummaryFromServer(false);
            AsyncManager.getInstance().executeOnceInSeconds(service, 60);

        }
    }
    
    public void refreshSessionDataAndTree() {
        SwingUtilities.invokeLater(() -> {
            try {
                // fetch the session summary
                updateSessionSummaryFromServer(true /*rebuildTree*/);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Refresh session data error: {0}", ex.getMessage());
            }
        });
    }

    public void updateSessionSummaryFromServer(boolean rebuildTree) {
        SessionSummary summary = SessionDataManager.getSessionSummaryData();

        String jwt = FileUtilManager.getItem("jwt");
        String api = "/sessions/summary?refresh=true";
        ClientResponse resp = OpsHttpClient.softwareGet(api, jwt);
        if (resp.isOk()) {
            JsonObject jsonObj = resp.getJsonObj();

            Type type = new TypeToken<SessionSummary>() {}.getType();
            SessionSummary fetchedSummary = SoftwareUtil.gson.fromJson(jsonObj, type);

            // clone all
            summary.clone(fetchedSummary);

            TimeDataManager.updateSessionFromSummaryApi(fetchedSummary.getCurrentDayMinutes());

            // save the file
            FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), summary);
        }
        
        dispatchStatusViewUpdate(rebuildTree);
    }

    private void updateWallClockTime() {
        // pass control from a background thread to the event dispatch thread,
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean isActive = true; // TODO: figure out how to get focus state
                if (isActive && SoftwareEventManager.isCurrentlyActive) {
                    long wctime = getWcTimeInSeconds() + SECONDS_INCREMENT;
                    FileUtilManager.setNumericItem("wctime", wctime);

                    // update the json time data file
                    TimeDataManager.incrementEditorSeconds(SECONDS_INCREMENT);
                }
                dispatchStatusViewUpdate(false);
            }
        });
    }

    public synchronized void dispatchStatusViewUpdate(boolean rebuildTree) {
        if (!dispatching) {
            dispatching = true;

            // STATUS BAR REFRESH
            StatusBarManager.updateStatusBar();

            if (!rebuildTree) {
                CodeTimeTreeTopComponent.refreshTree();
            } else {
                CodeTimeTreeTopComponent.rebuildTree();
            }
        }
        dispatching = false;
    }

    private void clearWcTime() {
        setWcTime(0);
    }

    public long getWcTimeInSeconds() {
        return FileUtilManager.getNumericItem("wctime", 0);
    }

    public void setWcTime(long seconds) {
        FileUtilManager.setNumericItem("wctime", seconds);
        updateWallClockTime();
    }

}
