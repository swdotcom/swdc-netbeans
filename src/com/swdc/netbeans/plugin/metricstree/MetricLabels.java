/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.SessionSummary;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MetricLabels {
    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
    
    public String keystrokes = "";
    public String keystrokesAvg = "";
    public String keystrokesGlobalAvg = "";
    public String keystrokesAvgIcon = "";
    public String linesAdded = "";
    public String linesAddedAvg = "";
    public String linesAddedGlobalAvg = "";
    public String linesAddedAvgIcon = "";
    public String linesRemoved = "";
    public String linesRemovedAvg = "";
    public String linesRemovedGlobalAvg = "";
    public String linesRemovedAvgIcon = "";
    public String activeCodeTime = "";
    public String activeCodeTimeAvg = "";
    public String activeCodeTimeGlobalAvg = "";
    public String activeCodeTimeAvgIcon = "";
    public String codeTime = "";

    public void updateLabels(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());

        if (sessionSummary != null) {
            keystrokes = "Today: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes());
            keystrokesAvg = "Your average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageDailyKeystrokes());
            keystrokesGlobalAvg = "Global average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageDailyKeystrokes());
            keystrokesAvgIcon = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.png" : "bolt-grey.png";

            linesAdded = "Today: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded());
            linesAddedAvg = "Your average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageLinesAdded());
            linesAddedGlobalAvg = "Global average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
            linesAddedAvgIcon = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.png" : "bolt-grey.png";

            linesRemoved = "Today: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved());
            linesRemovedAvg = "Your average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageLinesRemoved());
            linesRemovedGlobalAvg = "Global average (" + dayStr + "): " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesRemoved());
            linesRemovedAvgIcon = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.png";
        }

        if (codeTimeSummary != null && sessionSummary != null) {
            activeCodeTime = "Today: " + SoftwareUtil.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
            activeCodeTimeAvg = "Your average (" + dayStr + "): " + SoftwareUtil.humanizeMinutes((int) sessionSummary.getAverageDailyMinutes());
            activeCodeTimeGlobalAvg = "Global average (" + dayStr + "): " + SoftwareUtil.humanizeMinutes((int) sessionSummary.getGlobalAverageDailyMinutes());
            activeCodeTimeAvgIcon = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.png" : "bolt-grey.png";

            codeTime = "Today: " + SoftwareUtil.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        }
    }
}
