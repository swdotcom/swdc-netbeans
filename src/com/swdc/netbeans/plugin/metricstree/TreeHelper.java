/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.OsaScriptManager;
import com.swdc.netbeans.plugin.managers.SlackClientManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.managers.SwitchAccountManager;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.netbeans.plugin.models.Integration;
import com.swdc.netbeans.plugin.models.SlackDndInfo;
import com.swdc.netbeans.plugin.models.SlackUserPresence;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSeparator;
import org.apache.commons.lang.StringUtils;
import org.openide.awt.HtmlBrowser;


public class TreeHelper {
    
    public static final Logger LOG = Logger.getLogger("TreeHelper");
    
    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHIUB_SIGNUP_ID = "github";
    public static final String EMAIL_SIGNUP_ID = "email";
    public static final String LOGGED_IN_ID = "logged_in";
    public static final String LEARN_MORE_ID = "learn_more";
    public static final String SEND_FEEDBACK_ID = "send_feedback";
    public static final String ADVANCED_METRICS_ID = "advanced_metrics";
    public static final String TOGGLE_METRICS_ID = "toggle_metrics";
    public static final String VIEW_SUMMARY_ID = "view_summary";
    public static final String CODETIME_PARENT_ID = "codetime_parent";
    public static final String CODETIME_TODAY_ID = "codetime_today";
    public static final String ACTIVE_CODETIME_PARENT_ID = "active_codetime_parent";
    public static final String ACTIVE_CODETIME_TODAY_ID = "active_codetime_today";
    public static final String ACTIVE_CODETIME_AVG_TODAY_ID = "active_codetime_avg_today";
    public static final String ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID = "active_codetime_global_avg_today";
    
    public static final String LINES_ADDED_TODAY_ID = "lines_added_today";
    public static final String LINES_ADDED_AVG_TODAY_ID = "lines_added_avg_today";
    public static final String LINES_ADDED_GLOBAL_AVG_TODAY_ID = "lines_added_global_avg_today";
    
    public static final String LINES_DELETED_TODAY_ID = "lines_deleted_today";
    public static final String LINES_DELETED_AVG_TODAY_ID = "lines_deleted_avg_today";
    public static final String LINES_DELETED_GLOBAL_AVG_TODAY_ID = "lines_deleted_global_avg_today";
    
    public static final String KEYSTROKES_TODAY_ID = "keystrokes_today";
    public static final String KEYSTROKES_AVG_TODAY_ID = "keystrokes_avg_today";
    public static final String KEYSTROKES_GLOBAL_AVG_TODAY_ID = "keystrokes_global_avg_today";
    
    public static final String SWITCH_ACCOUNT_ID = "switch_account";
    
    public static final String SLACK_WORKSPACES_NODE_ID = "slack_workspaces_node";
    public static final String SWITCH_OFF_DARK_MODE_ID = "switch_off_dark_mode";
    public static final String SWITCH_ON_DARK_MODE_ID = "switch_ON_dark_mode";
    public static final String TOGGLE_DOCK_POSITION_ID = "toggle_dock_position";
    public static final String SWITCH_OFF_DND_ID = "switch_off_dnd";
    public static final String SWITCH_ON_DND_ID = "switch_on_dnd";
    public static final String CONNECT_SLACK_ID = "connect_slack";
    public static final String ADD_WORKSPACE_ID = "add_workspace";
    public static final String SET_PRESENCE_AWAY_ID = "set_presence_away";
    public static final String SET_PRESENCE_ACTIVE_ID = "set_presence_active";
    

    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
    
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
    
    public static MetricTreeNode buildLoggedInNode() {
        String authType = FileManager.getItem("authType");
        String name = FileManager.getItem("name");
        String iconName = "icons8-envelope-16.png";
        if ("google".equals(authType)) {
            iconName = "google.png";
        } else if ("github".equals(authType)) {
            iconName = "github.png";
        }
        
        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID);
        node.add(new MetricTreeNode("Switch account", "paw.png", SWITCH_ACCOUNT_ID));
        return node;
    }
    
    public static List<MetricTreeNode> buildMenuNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        
        String toggleText = "Hide status bar metrics";
        if (!SoftwareUtil.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }
        
        list.add(new MetricTreeNode("Learn more", "readme.png", LEARN_MORE_ID));
        list.add(new MetricTreeNode("Submit feedback", "message.png", SEND_FEEDBACK_ID));
        list.add(new MetricTreeNode(toggleText, "visible.png", TOGGLE_METRICS_ID));
        
        list.add(buildSlackWorkspacesNode());
        
        return list;
    }
    
    public static MetricTreeNode buildSummaryButton() {
        return new MetricTreeNode("Dashboard", "dashboard.png", VIEW_SUMMARY_ID);
    }
    
    public static MetricTreeNode buildViewWebDashboardButton() {
        return new MetricTreeNode("More data at Software.com", "paw-grey.png", ADVANCED_METRICS_ID);
    }
    
    public static List<MetricTreeNode> buildTreeFlowNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        
        if (SlackClientManager.hasSlackWorkspaces()) {
            SlackDndInfo slackDndInfo = SlackClientManager.getSlackDnDInfo();
            
            // snooze node
            if (slackDndInfo.snooze_enabled) {
                list.add(getSwitchOnDndNode());
            } else {
                list.add(getSwitchOffDndNode());
            }
            // presence toggle
            SlackUserPresence slackUserPresence = SlackClientManager.getSlackUserPresence();
            if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
                list.add(getSetAwayPresenceNode());
            } else {
                list.add(getSetActivePresenceNode());
            }
        } else {
            // show the connect slack node
            list.add(getConnectSlackNode());
        }
        
        if (SoftwareUtil.isMac()) {
            if (OsaScriptManager.isDarkMode()) {
                list.add(getSwitchOffDarkModeNode());
            } else {
                list.add(getSwitchOnDarkModeNode());
            }
            
            list.add(new MetricTreeNode("Toggle dock position", "", TOGGLE_DOCK_POSITION_ID));
        }
        
        return list;
    }
    
    public static MetricTreeNode getSwitchOffDarkModeNode() {
        return new MetricTreeNode("Turn off dark mode", "", SWITCH_OFF_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getSwitchOnDarkModeNode() {
        return new MetricTreeNode("Turn on dark mode", "icons8-night-16.png", SWITCH_ON_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getConnectSlackNode() {
        return new MetricTreeNode("Connect to set your status and pause notifications", "icons8-slack-new-16.png", CONNECT_SLACK_ID);
    }
    
    public static MetricTreeNode getSwitchOffDndNode() {
        return new MetricTreeNode("Turn off notifications", "", SWITCH_OFF_DND_ID);
    }
    
    public static MetricTreeNode getSwitchOnDndNode() {
        return new MetricTreeNode("Turn on notifications", "", SWITCH_ON_DND_ID);
    }
    
    public static MetricTreeNode getSetAwayPresenceNode() {
        return new MetricTreeNode("Set presence to away", "", SET_PRESENCE_AWAY_ID);
    }
    
    public static MetricTreeNode getSetActivePresenceNode() {
        return new MetricTreeNode("Set presence to active", "", SET_PRESENCE_ACTIVE_ID);
    }
    
    public static MetricTreeNode buildSlackWorkspacesNode() {
        MetricTreeNode node = new MetricTreeNode("Slack workspaces", null, SLACK_WORKSPACES_NODE_ID);
        List<Integration> workspaces = SlackClientManager.getSlackWorkspaces();
        workspaces.forEach(workspace -> {
            node.add(new MetricTreeNode(workspace.team_domain, "icons8-slack-new-16.png", workspace.authId));
        });
        // add the add new workspace button
        node.add(getAddSlackWorkspaceNode());
        return node;
    }
    
    public static MetricTreeNode getAddSlackWorkspaceNode() {
        return new MetricTreeNode("Add workspace", "add.png", ADD_WORKSPACE_ID);
    }
    
    public static MetricTreeNode buildActiveCodeTimeTree(MetricLabels mLabels) {
        MetricTreeNode treeNode = new MetricTreeNode("Active code time", null, ACTIVE_CODETIME_PARENT_ID);
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTime, "rocket.png", ACTIVE_CODETIME_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTimeAvg, mLabels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.activeCodeTimeGlobalAvg, "global-grey.png", ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID));
        return treeNode;
    }
    
    public static MetricTreeNode buildCodeTimeTree(MetricLabels mLabels) {
        MetricTreeNode treeNode = new MetricTreeNode("Code time", null, CODETIME_PARENT_ID);
        treeNode.add(new MetricTreeNode(mLabels.codeTime, "rocket.png", CODETIME_TODAY_ID));
        return treeNode;
    }
    
    public static MetricTreeNode buildLinesAddedTree(MetricLabels mLabels) {
        // create the lines added nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines added", null, null);
        treeNode.add(new MetricTreeNode(mLabels.linesAdded, "rocket.png", LINES_ADDED_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesAddedAvg, mLabels.linesAddedAvgIcon, LINES_ADDED_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesAddedGlobalAvg, "global-grey.png", LINES_ADDED_GLOBAL_AVG_TODAY_ID));

        return treeNode;
    }

    public static MetricTreeNode buildLinesRemovedTree(MetricLabels mLabels) {
        // create the lines removed nodes
        MetricTreeNode treeNode = new MetricTreeNode("Lines removed", null, null);
        treeNode.add(new MetricTreeNode(mLabels.linesRemoved, "rocket.png", LINES_DELETED_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesRemovedAvg, mLabels.linesRemovedAvgIcon, LINES_DELETED_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.linesRemovedGlobalAvg, "global-grey.png", LINES_DELETED_GLOBAL_AVG_TODAY_ID));
        return treeNode;
    }

    public static MetricTreeNode buildKeystrokesTree(MetricLabels mLabels) {
        // create the keystrokes nodes
        MetricTreeNode treeNode = new MetricTreeNode("Keystrokes", null, null);
        treeNode.add(new MetricTreeNode(mLabels.keystrokes, "rocket.png", KEYSTROKES_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.keystrokesAvg, mLabels.keystrokesAvgIcon, KEYSTROKES_AVG_TODAY_ID));
        treeNode.add(new MetricTreeNode(mLabels.keystrokesGlobalAvg, "global-grey.png", KEYSTROKES_GLOBAL_AVG_TODAY_ID));
        
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
        MetricTreeNode treeNode = new MetricTreeNode(parentName, null, getTopFileParentId(sortBy));

        addNodesToTopFilesMetricParentTreeNode(treeNode, sortBy, fileChangeInfoMap);

        return treeNode;
    }
    
    public static void addNodesToTopFilesMetricParentTreeNode(MetricTreeNode treeNode, String sortBy, Map<String, FileChangeInfo> fileChangeInfoMap) {
        // build the most edited files nodes
        // sort the fileChangeInfoMap based on keystrokes
        List<Map.Entry<String, FileChangeInfo>> entryList = null;
        if (!fileChangeInfoMap.isEmpty()) {
            if (sortBy.equals("kpm")) {
                entryList = sortByKpm(fileChangeInfoMap);
            } else if (sortBy.equals("keystrokes")) {
                entryList = sortByKeystrokes(fileChangeInfoMap);
            } else if (sortBy.equals("codetime")) {
                entryList = sortByFileSeconds(fileChangeInfoMap);
            } else {
                entryList = new ArrayList<>(fileChangeInfoMap.entrySet());
            }
        }
        
        if (entryList != null && entryList.size() > 0) {
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
                MetricTreeNode editedFileNode = new MetricTreeNode(label, "files.png", getTopFilesId(name, sortBy));
                editedFileNode.setData(fileChangeInfoEntry.getValue());
                treeNode.add(editedFileNode);
                count++;
            }
        } else {
            MetricTreeNode node = new MetricTreeNode("<empty>", "files.png", null);
            treeNode.add(node);
        }
    }
    
    public static String getTopFileParentId(String sortBy) {
        return sortBy.toLowerCase() + "_topfiles_parent";
    }
    
    public static String getTopFilesId(String name, String sortBy) {
        String id = name.replaceAll("\\s", "_") + "_" + sortBy;
        return id.toLowerCase();
    }

    private static void launchFileClick(MetricTreeNode selectedNode) {
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
    
    public static void handleClickEvent(MetricTreeNode node) {
        switch (node.getId()) {
            case GOOGLE_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("google", UIInteractionType.click, false);
                break;
            case GITHIUB_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("github", UIInteractionType.click, false);
                break;
            case EMAIL_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("email", UIInteractionType.click, false);
                break;
            case LOGGED_IN_ID:
                CodeTimeTreeTopComponent.expandCollapse(LOGGED_IN_ID);
                break;
            case SWITCH_ACCOUNT_ID:
                SwitchAccountManager.initiateSwitchAccountFlow();
                break;
            case VIEW_SUMMARY_ID:
                SoftwareUtil.launchCodeTimeMetricsDashboard();
                break;
            case TOGGLE_METRICS_ID:
                SoftwareUtil.toggleStatusBar(UIInteractionType.click);
                CodeTimeTreeTopComponent.updateMetrics(null, null);
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
            case CONNECT_SLACK_ID:
            case ADD_WORKSPACE_ID:
                SlackClientManager.connectSlackWorkspace();
                break;
            case SWITCH_OFF_DARK_MODE_ID:
            case SWITCH_ON_DARK_MODE_ID:
                OsaScriptManager.toggleDarkMode();
                break;
            case SWITCH_OFF_DND_ID:
                SlackClientManager.pauseSlackNotifications();
                break;
            case SWITCH_ON_DND_ID:
                SlackClientManager.enableSlackNotifications();
                break;
            case SET_PRESENCE_ACTIVE_ID:
                SlackClientManager.toggleSlackPresence("auto");
                break;
            case SET_PRESENCE_AWAY_ID:
                SlackClientManager.toggleSlackPresence("away");
                break;
            case TOGGLE_DOCK_POSITION_ID:
                OsaScriptManager.toggleDock();
                break;
            default:
                launchFileClick(node);
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
