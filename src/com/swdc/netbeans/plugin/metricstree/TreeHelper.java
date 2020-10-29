/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import static com.swdc.netbeans.plugin.SoftwareUtil.LOG;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.netbeans.plugin.models.SessionSummary;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.openide.awt.HtmlBrowser;

/**
 *
 * @author xavierluiz
 */
public class TreeHelper {
    
    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHIUB_SIGNUP_ID = "github";
    public static final String EMAIL_SIGNUP_ID = "email";
    public static final String LOGGED_IN_ID = "logged_in";
    public static final String LEARN_MORE_ID = "learn_more";
    public static final String SEND_FEEDBACK_ID = "send_feedback";
    public static final String ADVANCED_METRICS_ID = "advanced_metrics";
    public static final String TOGGLE_METRICS_ID = "toggle_metrics";
    public static final String VIEW_SUMMARY_ID = "view_summary";
    public static final String CODETIME_TODAY_ID = "codetime_today";

    private static SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
    
    public static List<MetricTreeNode> buildSignupNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        String name = FileManager.getItem("name");
        if (name == null || name.equals("")) {
            list.add(buildSignupNode("google"));
            list.add(buildSignupNode("github"));
            list.add(buildSignupNode("email"));
        } else {
            list.add(buildLoggedInNode());
        }
        return list;
    }
    
    private static MetricTreeNode buildSignupNode(String type) {
        String iconName = "icons8-envelope-16.png";
        String text = "Sign up with email";
        String id = EMAIL_SIGNUP_ID;
        if (type.equals("google")) {
            iconName = "google.png";
            text = "Sign up with Google";
            id = GOOGLE_SIGNUP_ID;
        } else if (type.equals("github")) {
            iconName = "github.png";
            text = "Sign up with GitHub";
            id = GITHIUB_SIGNUP_ID;
        }
        MetricTreeNode node = new MetricTreeNode(text, iconName, id);
        return node;
    }
    
    private static MetricTreeNode buildLoggedInNode() {
        String authType = FileManager.getItem("authType");
        String name = FileManager.getItem("name");
        String iconName = "icons8-envelope-16.png";
        if ("google".equals(authType)) {
            iconName = "google.png";
        } else if ("github".equals(authType)) {
            iconName = "github.png";
        }
        
        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID);
        return node;
    }
    
    public static List<MetricTreeNode> buildMenuNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        
        String toggleText = "Hide status bar metrics";
        if (!SoftwareUtil.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }
        list.add(new MetricTreeNode(toggleText, "visible.png", TOGGLE_METRICS_ID));
        list.add(new MetricTreeNode("Learn more", "readme.png", LEARN_MORE_ID));
        list.add(new MetricTreeNode("Submit feedback", "message.png", SEND_FEEDBACK_ID));
        list.add(new MetricTreeNode("See advanced metrics", "paw-grey.png", ADVANCED_METRICS_ID));
        list.add(new MetricTreeNode("View summary", "dashboard.png", VIEW_SUMMARY_ID));
        
        return list;
    }
    
    public static MetricTreeNode buildActiveCodeTimeTree(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        MetricTreeNode activeCodeTime = new MetricTreeNode("Active code time");
        
        String min = SoftwareUtil.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
        String avg = SoftwareUtil.humanizeMinutes((int) sessionSummary.getAverageDailyMinutes());
        String globalAvg = SoftwareUtil.humanizeMinutes((int) sessionSummary.getGlobalAverageDailyMinutes());
        
        String dayStr = formatDay.format(new Date());
        
        activeCodeTime.add(new MetricTreeNode("Today: " + min, "rocket.png"));
        String avgIconName = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.png" : "bolt-grey.png";
        activeCodeTime.add(new MetricTreeNode("Your average (" + dayStr + "): " + avg, avgIconName));
        activeCodeTime.add(new MetricTreeNode("Global average (" + dayStr + "): " + globalAvg, "global-grey.svg"));
        
        return activeCodeTime;
    }
    
    public static MetricTreeNode buildCodeTimeTree(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        MetricTreeNode codeTime = new MetricTreeNode("Code time");
        
        String min = SoftwareUtil.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        
        String dayStr = formatDay.format(new Date());
        
        codeTime.add(new MetricTreeNode("Today: " + min, "rocket.png"));

        return codeTime;
    }

    private static void launchFileClick(MouseEvent e) {
        MetricTree mTree = (MetricTree)e.getSource();
        if (mTree.getLeadSelectionPath() != null) {
            MetricTreeNode selectedNode = (MetricTreeNode) mTree.getLeadSelectionPath().getLastPathComponent();
            if (selectedNode != null) {
                if (selectedNode.getData() != null && selectedNode.getData() instanceof FileChangeInfo) {
                    String fsPath = ((FileChangeInfo) selectedNode.getData()).fsPath;
                    SoftwareUtil.launchFile(fsPath);
                } else if (selectedNode.getPath() != null && selectedNode.getData() instanceof String && String.valueOf(selectedNode.getData()).contains("http")) {
                    // launch the commit url
                    String url = String.valueOf(selectedNode.getData());
                    
                    try {
                        URL launchUrl = new URL(url);
                        HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
                    } catch (MalformedURLException ex) {
                        LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, ex.getMessage()});
                    }
                }
            }
        }
    }
    
    private static final List<String> toggleItems = Arrays.asList("ct_codetime_toggle_node",
            "ct_active_codetime_toggle_node",
            "ct_lines_added_toggle_node",
            "ct_lines_removed_toggle_node",
            "ct_keystrokes_toggle_node",
            "ct_files_changed_toggle_node",
            "ct_top_files_by_kpm_toggle_node",
            "ct_top_files_by_keystrokes_toggle_node",
            "ct_top_files_by_codetime_toggle_node",
            "ct_open_changes_toggle_node",
            "ct_committed_today_toggle_node");
    
    private static String getToggleItem(String normalizedLabel) {
        for (String toggleItem : toggleItems) {
            // strip off "ct_" and "_toggle_node" and replace the "_" with ""
            String normalizedToggleItem = toggleItem.replace("ct_", "").replace("_toggle_node", "").replaceAll("_", "");
            if (normalizedLabel.toLowerCase().indexOf(normalizedToggleItem) != -1) {
                return toggleItem;
            }
        }
        return null;
    }
    
    public static void handleClickEvent(MetricTreeNode node) {
        switch (node.getId()) {
            case GOOGLE_SIGNUP_ID:
            case GITHIUB_SIGNUP_ID:
            case EMAIL_SIGNUP_ID:
                break;
            case LOGGED_IN_ID:
                break;
            case VIEW_SUMMARY_ID:
                break;
            case TOGGLE_METRICS_ID:
                SoftwareUtil.toggleStatusBar(UIInteractionType.click);
                break;
            case SEND_FEEDBACK_ID:
                break;
            case LEARN_MORE_ID:
                break;
            default:
                break;
        }
    }
    
}
