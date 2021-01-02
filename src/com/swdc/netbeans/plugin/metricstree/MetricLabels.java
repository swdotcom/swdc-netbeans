/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.SessionSummary;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MetricLabels {
    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
    
    public String keystrokes = "";
    public String keystrokesReferenceAvg = "";
    public String keystrokesAvg = "";
    public String keystrokesGlobalAvg = "";
    public String keystrokesAvgIcon = "";
    public String linesAdded = "";
    public String linesAddedReferenceAvg = "";
    public String linesAddedAvg = "";
    public String linesAddedGlobalAvg = "";
    public String linesAddedAvgIcon = "";
    public String linesRemoved = "";
    public String linesRemovedReferenceAvg = "";
    public String linesRemovedAvg = "";
    public String linesRemovedGlobalAvg = "";
    public String linesRemovedAvgIcon = "";
    public String activeCodeTime = "";
    public String activeCodeTimeReferenceAvg = "";
    public String activeCodeTimeAvg = "";
    public String activeCodeTimeGlobalAvg = "";
    public String activeCodeTimeAvgIcon = "";
    public String codeTime = "";

    public void updateLabels(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());
        String refClass = FileManager.getItem("reference-class", "user");
        long referenceValue = 0;

        if (sessionSummary != null) {
            referenceValue = refClass.equals("user") ? sessionSummary.getAverageDailyKeystrokes() : sessionSummary.getGlobalAverageDailyKeystrokes();
            keystrokesReferenceAvg = getPercentOfReferenceAvg(sessionSummary.getCurrentDayKeystrokes(), referenceValue);
            keystrokes = "Keystrokes: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes()) + " " + keystrokesReferenceAvg;

            keystrokesAvgIcon = referenceValue < sessionSummary.getCurrentDayKeystrokes() ? "bolt.png" : "bolt-grey.png";

            referenceValue = refClass.equals("user") ? sessionSummary.getAverageLinesAdded() : sessionSummary.getGlobalAverageLinesAdded();
            linesAddedReferenceAvg = getPercentOfReferenceAvg(sessionSummary.getCurrentDayLinesAdded(), referenceValue);
            linesAdded = "Lines added: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded()) + " " + linesAddedReferenceAvg;

            linesAddedAvgIcon = referenceValue < sessionSummary.getCurrentDayLinesAdded() ? "bolt.png" : "bolt-grey.png";

            referenceValue = refClass.equals("user") ? sessionSummary.getAverageLinesRemoved() : sessionSummary.getGlobalAverageLinesRemoved();
            linesRemovedReferenceAvg = getPercentOfReferenceAvg(sessionSummary.getCurrentDayLinesRemoved(), referenceValue);
            linesRemoved = "Lines removed: " + SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved()) + " " + linesRemovedReferenceAvg;

            linesRemovedAvgIcon = referenceValue < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.png";
        }

        if (codeTimeSummary != null && sessionSummary != null) {
            // Active code time
            referenceValue = refClass.equals("user") ? sessionSummary.getAverageDailyMinutes() : sessionSummary.getGlobalAverageDailyMinutes();
            activeCodeTimeReferenceAvg = getPercentOfReferenceAvg(codeTimeSummary.activeCodeTimeMinutes, referenceValue);
            
            activeCodeTime = "Active code time: " + SoftwareUtil.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes) + " " + activeCodeTimeReferenceAvg;

            activeCodeTimeAvgIcon = referenceValue < sessionSummary.getCurrentDayMinutes() ? "bolt.png" : "bolt-grey.png";

            // Code Time
            codeTime = "Code time: " + SoftwareUtil.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        }
    }
    
    private String getPercentOfReferenceAvg(long currentValue, long referenceValue) {
        if (currentValue == 0 && referenceValue == 0) {
            return "";
        }
        double quotient = 1;
        if (referenceValue > 0) {
            quotient = currentValue / referenceValue;
            if (currentValue > 0 && quotient < 0.01) {
                quotient = 0.01;
            }
        }
        return "(" + String.format("%.0f", (quotient * 100)) + "% of avg)";
    }
    
}
