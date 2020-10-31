/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import static com.swdc.netbeans.plugin.SoftwareUtil.LOG;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.netbeans.plugin.models.SessionSummary;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JSeparator;
import org.apache.commons.lang.StringUtils;
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
        MetricTreeNode node = new MetricTreeNode(text, iconName, id, true);
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
        
        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID, true);
        return node;
    }
    
    public static List<MetricTreeNode> buildMenuNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        
        String toggleText = "Hide status bar metrics";
        if (!SoftwareUtil.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }
        list.add(new MetricTreeNode(toggleText, "visible.png", TOGGLE_METRICS_ID, true));
        list.add(new MetricTreeNode("Learn more", "readme.png", LEARN_MORE_ID, true));
        list.add(new MetricTreeNode("Submit feedback", "message.png", SEND_FEEDBACK_ID, true));
        list.add(new MetricTreeNode("See advanced metrics", "paw-grey.png", ADVANCED_METRICS_ID, true));
        list.add(new MetricTreeNode("View summary", "dashboard.png", VIEW_SUMMARY_ID, true));
        
        return list;
    }
    
    public static MetricTreeNode buildActiveCodeTimeTree(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        MetricTreeNode treeNode = new MetricTreeNode("Active code time", false);
        
        String min = SoftwareUtil.humanizeMinutes(codeTimeSummary.activeCodeTimeMinutes);
        String avg = SoftwareUtil.humanizeMinutes((int) sessionSummary.getAverageDailyMinutes());
        String globalAvg = SoftwareUtil.humanizeMinutes((int) sessionSummary.getGlobalAverageDailyMinutes());
        
        String dayStr = formatDay.format(new Date());
        
        treeNode.add(new MetricTreeNode("Today: " + min, "rocket.png", true));
        String avgIconName = sessionSummary.getAverageDailyMinutes() < sessionSummary.getCurrentDayMinutes() ? "bolt.png" : "bolt-grey.png";
        treeNode.add(new MetricTreeNode("Your average (" + dayStr + "): " + avg, avgIconName, true));
        treeNode.add(new MetricTreeNode("Global average (" + dayStr + "): " + globalAvg, "global-grey.svg", true));
        
        return treeNode;
    }
    
    public static MetricTreeNode buildCodeTimeTree(CodeTimeSummary codeTimeSummary) {
        MetricTreeNode treeNode = new MetricTreeNode("Code time", false);
        
        String min = SoftwareUtil.humanizeMinutes(codeTimeSummary.codeTimeMinutes);
        treeNode.add(new MetricTreeNode("Today: " + min, "rocket.png", true));

        return treeNode;
    }
    
    public static MetricTreeNode buildLinesAddedTree(SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());
        
        // create the lines added nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines added", false);
        String linesAdded = SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesAdded());
        String avgLinesAdded = SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageLinesAdded());
        String globalAvgLinesAdded = SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        treeNode.add(new MetricTreeNode("Today: " + linesAdded, "rocket.png", true));
        String avgIconName = sessionSummary.getAverageLinesAdded() < sessionSummary.getCurrentDayLinesAdded() ? "bolt.png" : "bolt-grey.png";
        treeNode.add(new MetricTreeNode("Your average (" + dayStr + "): " + avgLinesAdded, avgIconName, true));
        treeNode.add(new MetricTreeNode("Global average (" + dayStr + "): " + globalAvgLinesAdded, "global-grey.png", true));

        return treeNode;
    }

    public static MetricTreeNode buildLinesRemovedTree(SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());
        // create the lines removed nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines removed", false);
        String linesRemoved = SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayLinesRemoved());
        String avgLinesRemoved = SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageLinesRemoved());
        String globalAvgLinesRemoved = SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageLinesAdded());
        treeNode.add(new MetricTreeNode("Today: " + linesRemoved, "rocket.png", true));
        String avgIconName = sessionSummary.getAverageLinesRemoved() < sessionSummary.getCurrentDayLinesRemoved() ? "bolt.svg" : "bolt-grey.png";
        treeNode.add(new MetricTreeNode("Your average (" + dayStr + "): " + avgLinesRemoved, avgIconName, true));
        treeNode.add(new MetricTreeNode("Global average (" + dayStr + "): " + globalAvgLinesRemoved, "global-grey.png", true));
        return treeNode;
    }

    public static MetricTreeNode buildKeystrokesTree(SessionSummary sessionSummary) {
        String dayStr = formatDay.format(new Date());
        // create the keystrokes nodes
        MetricTreeNode treeNode = new MetricTreeNode("Keystrokes", false);
        String keystrokes = SoftwareUtil.humanizeLongNumbers(sessionSummary.getCurrentDayKeystrokes());
        String avgKeystrokes = SoftwareUtil.humanizeLongNumbers(sessionSummary.getAverageDailyKeystrokes());
        String globalKeystrokes = SoftwareUtil.humanizeLongNumbers(sessionSummary.getGlobalAverageDailyKeystrokes());
        treeNode.add(new MetricTreeNode("Today: " + keystrokes, "rocket.png", true));
        String avgIconName = sessionSummary.getAverageDailyKeystrokes() < sessionSummary.getCurrentDayKeystrokes() ? "bolt.png" : "bolt-grey.png";
        treeNode.add(new MetricTreeNode("Your average (" + dayStr + "): " + avgKeystrokes, avgIconName, true));
        treeNode.add(new MetricTreeNode("Global average (" + dayStr + "): " + globalKeystrokes, "global-grey.png", true));
        return treeNode;
    }
    
    public static MetricTreeNode buildTopKeystrokesFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by keystrokes", "keystrokes", fileChangeInfoMap);
    }

    public static MetricTreeNode buildTopKpmFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by KPM", "kpm", fileChangeInfoMap);
    }

    public static MetricTreeNode buildTopCodeTimeFilesTree(Map<String, FileChangeInfo> fileChangeInfoMap) {
        return buildTopFilesTree("Top files by code time", "codetime", fileChangeInfoMap);
    }
    
    private static MetricTreeNode buildTopFilesTree(String parentName, String sortBy, Map<String, FileChangeInfo> fileChangeInfoMap) {
        MetricTreeNode treeNode = new MetricTreeNode(parentName, false);
        if (fileChangeInfoMap.size() == 0) {
            return null;
        }
        // build the most edited files nodes
        List<MetricTreeNode> nodes = new ArrayList<>();
        // sort the fileChangeInfoMap based on keystrokes
        List<Map.Entry<String, FileChangeInfo>> entryList = null;

        if (sortBy.equals("kpm")) {
            entryList = sortByKpm(fileChangeInfoMap);
        } else if (sortBy.equals("keystrokes")) {
            entryList = sortByKeystrokes(fileChangeInfoMap);
        } else if (sortBy.equals("codetime")) {
            entryList = sortByFileSeconds(fileChangeInfoMap);
        }

        int count = 0;
        // go from the end
        for (int i = entryList.size() - 1; i >= 0; i--) {
            if (count >= 3) {
                break;
            }
            Map.Entry<String, FileChangeInfo> fileChangeInfoEntry = entryList.get(i);
            String name = fileChangeInfoEntry.getValue().name;
            if (StringUtils.isBlank(name)) {
                Path path = Paths.get(fileChangeInfoEntry.getKey());
                if (path != null) {
                    Path fileName = path.getFileName();
                    if (fileName != null) {
                        name = fileName.toString();
                    } else {
                        name = "Untitled";
                    }
                }
            }

            String val = "";
            if (sortBy.equals("kpm")) {
                val = SoftwareUtil.humanizeLongNumbers(fileChangeInfoEntry.getValue().kpm);
            } else if (sortBy.equals("keystrokes")) {
                val = SoftwareUtil.humanizeLongNumbers(fileChangeInfoEntry.getValue().keystrokes);
            } else if (sortBy.equals("codetime")) {
                val = SoftwareUtil.humanizeMinutes((int) (fileChangeInfoEntry.getValue().duration_seconds / 60));
            }

            String label = name + " | " + val;
            MetricTreeNode editedFileNode = new MetricTreeNode(label, "files.png", true);
            editedFileNode.setData(fileChangeInfoEntry.getValue());
            treeNode.add(editedFileNode);
            count++;
        }

        return treeNode;
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
            case ADVANCED_METRICS_ID:
                SoftwareSessionManager.launchWebDashboard(UIInteractionType.click);
                break;
            case SEND_FEEDBACK_ID:
                SoftwareSessionManager.submitFeedback(UIInteractionType.click);
                break;
            case LEARN_MORE_ID:
                FileManager.openReadmeFile(UIInteractionType.click);
                break;
            default:
                break;
        }
    }
    
    private static List<Map.Entry<String, FileChangeInfo>> sortByKpm(Map<String, FileChangeInfo> fileChangeInfoMap) {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {

                        Long a = entryA.getValue().kpm;
                        Long b = entryB.getValue().kpm;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByKeystrokes(Map<String, FileChangeInfo> fileChangeInfoMap) {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {

                        Long a = entryA.getValue().keystrokes;
                        Long b = entryB.getValue().keystrokes;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }

    private static List<Map.Entry<String, FileChangeInfo>> sortByFileSeconds(Map<String, FileChangeInfo> fileChangeInfoMap) {
        List<Map.Entry<String, FileChangeInfo>> entryList = new ArrayList<Map.Entry<String, FileChangeInfo>>(fileChangeInfoMap.entrySet());
        // natural ASC order
        Collections.sort(
                entryList, new Comparator<Map.Entry<String, FileChangeInfo>>() {
                    @Override
                    public int compare(Map.Entry<String, FileChangeInfo> entryA,
                                       Map.Entry<String, FileChangeInfo> entryB) {
                        Long a = entryA.getValue().duration_seconds;
                        Long b = entryB.getValue().duration_seconds;
                        return a.compareTo(b);
                    }
                }
        );
        return entryList;
    }
    
    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }
    
}
