/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.managers;

import com.software.codetime.SoftwareUtil;
import static com.software.codetime.managers.SessionDataManager.getSessionSummaryFileData;
import com.software.codetime.status.SoftwareStatusBar;
import javax.swing.SwingUtilities;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.SessionSummary;

/**
 *
 * @author xavierluiz
 */
public class StatusBarManager {
    
    private static SoftwareStatusBar statusBar = new SoftwareStatusBar();
    private static boolean showingMetrics = true;
    
    public static boolean isShowingStatusBarMetrics() {
        return showingMetrics;
    }
    
    public static void updateStatusBar(SessionSummary sessionSummary) {
        updateStatusBar(sessionSummary, true);
    }
    
    public static void updateStatusBar(SessionSummary sessionSummary, final boolean showMetrics) {
        showingMetrics = showMetrics;
        if (sessionSummary == null) {
            sessionSummary = getSessionSummaryFileData();
        } else {
            // save the file
            FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), sessionSummary);
        }
        
        final SessionSummary summary = sessionSummary;
        SwingUtilities.invokeLater(() -> {
        	// update the status bar
    		String msg = UtilManager.humanizeMinutes(summary.currentDayMinutes);
                SoftwareStatusBar.StatusBarType type = SoftwareStatusBar.StatusBarType.OFF;
                if (showMetrics) {
                    type = summary.currentDayMinutes > summary.averageDailyMinutes ? SoftwareStatusBar.StatusBarType.ROCKET : SoftwareStatusBar.StatusBarType.PAW;
                }
    		StatusBarManager.setStatusLineMessage(type, msg, "Active code time today. Click to see more from Code Time.");
        });
    }
    
    public static void setStatusLineMessage(final SoftwareStatusBar.StatusBarType barType, final String statusMsg, final String tooltip) {
        statusBar.updateMessage(barType, statusMsg, tooltip);
    }
    
    public static void toggleStatusBarText() {
        showingMetrics = !showingMetrics;
        updateStatusBar(null, showingMetrics);
    }
    
    public static void updateTelementry(boolean telemetryOn) {
        SoftwareUtil.TELEMETRY_ON = telemetryOn;
        if (!SoftwareUtil.TELEMETRY_ON) {
            setStatusLineMessage(SoftwareStatusBar.StatusBarType.OFF, "<S> Paused", "Enable metrics to resume");
        } else {
            updateStatusBar(null);
        }
    }
}
