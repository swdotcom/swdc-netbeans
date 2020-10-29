/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import static com.swdc.netbeans.plugin.SoftwareUtil.TELEMETRY_ON;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar;

/**
 *
 * @author xavierluiz
 */
public class StatusBarManager {
    
    private static SoftwareStatusBar statusBar = new SoftwareStatusBar();
    
    public static void updateStatusBar() {
        
        CodeTimeSummary ctSummary = TimeDataManager.getCodeTimeSummary();

        SoftwareStatusBar.StatusBarType barType = SoftwareUtil.showingStatusText()
                ? SoftwareStatusBar.StatusBarType.PAW : SoftwareStatusBar.StatusBarType.OFF;
        String currentDayTimeStr = SoftwareUtil.humanizeMinutes(ctSummary.activeCodeTimeMinutes);
        
        StatusBarManager.setStatusLineMessage(barType, currentDayTimeStr, "Code time today. Click to see more from Code Time.");
    }
    
    public static void setStatusLineMessage(final SoftwareStatusBar.StatusBarType barType, final String statusMsg, final String tooltip) {
        statusBar.updateMessage(barType, statusMsg, tooltip);
    }
    
    public static void updateTelementry(boolean telemetryOn) {
        TELEMETRY_ON = telemetryOn;
        if (!TELEMETRY_ON) {
            setStatusLineMessage(SoftwareStatusBar.StatusBarType.OFF, "<S> Paused", "Enable metrics to resume");
        } else {
            updateStatusBar();
        }
    }
}
