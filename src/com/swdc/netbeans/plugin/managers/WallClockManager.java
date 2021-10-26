/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import java.util.logging.Logger;
import swdc.java.ops.manager.FileUtilManager;

public class WallClockManager {

    public static final Logger log = Logger.getLogger("WallClockManager");

    private static final int DAY_CHECK_TIMER_INTERVAL = 60 * 5;

    private static WallClockManager instance = null;
    private AsyncManager asyncManager;

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

        final Runnable newDayCheckerTimer = () -> newDayChecker();
        asyncManager.scheduleService(
                newDayCheckerTimer, "newDayCheckerTimer", 30, DAY_CHECK_TIMER_INTERVAL);
    }

    public void newDayChecker() {
        if (SoftwareUtil.isNewDay()) {
            
            SessionDataManager.clearSessionSummaryData();
            FileAggregateDataManager.clearFileChangeInfoSummaryData();

            // update the current day
            String day = SoftwareUtil.getTodayInStandardFormat();
            FileUtilManager.setItem("currentDay", day);

            // update the last payload timestamp
            FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", 0);
        }
    }

}
