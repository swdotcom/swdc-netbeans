/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.managers.AuthPromptManager;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSeparator;
import org.openide.awt.HtmlBrowser;
import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Integration;
import swdc.java.ops.model.MetricLabel;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;


public class TreeHelper {
    
    public static final Logger LOG = Logger.getLogger("TreeHelper");
    
    public static final String SIGN_UP_ID = "signup";
    public static final String LOG_IN_ID = "login";
    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHUB_SIGNUP_ID = "github";
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
        String name = FileUtilManager.getItem("name");
        if (name == null || name.equals("")) {
            list.add(new MetricTreeNode("Sign up", "signup.png", SIGN_UP_ID));
            list.add(new MetricTreeNode("Log in", "paw.png", LOG_IN_ID));
        } else {
            list.add(buildLoggedInNode());
        }
        return list;
    }
    
    public static MetricTreeNode buildLoggedInNode() {
        String authType = FileUtilManager.getItem("authType");
        String name = FileUtilManager.getItem("name");
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
        
        if (SlackManager.hasSlackWorkspaces()) {
            SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();
            
            // snooze node
            if (slackDndInfo.snooze_enabled) {
                list.add(getUnPausenotificationsNode(slackDndInfo));
            } else {
                list.add(getPauseNotificationsNode());
            }
            // presence toggle
            SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();
            if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
                list.add(getSetAwayPresenceNode());
            } else {
                list.add(getSetActivePresenceNode());
            }
        } else {
            // show the connect slack node
            list.add(getConnectSlackNode());
        }
        
        if (UtilManager.isMac()) {
            if (AppleScriptManager.isDarkMode()) {
                list.add(getSwitchOffDarkModeNode());
            } else {
                list.add(getSwitchOnDarkModeNode());
            }
            
            list.add(new MetricTreeNode("Toggle dock position", "settings.png", TOGGLE_DOCK_POSITION_ID));
        }
        
        return list;
    }
    
    public static MetricTreeNode getSwitchOffDarkModeNode() {
        return new MetricTreeNode("Turn off dark mode", "light-mode.png", SWITCH_OFF_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getSwitchOnDarkModeNode() {
        return new MetricTreeNode("Turn on dark mode", "dark-mode.png", SWITCH_ON_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getConnectSlackNode() {
        return new MetricTreeNode("Connect to set your status and pause notifications", "icons8-slack-new-16.png", CONNECT_SLACK_ID);
    }
    
    public static MetricTreeNode getPauseNotificationsNode() {
        return new MetricTreeNode("Pause notifications", "icons8-slack-new-16.png", SWITCH_OFF_DND_ID);
    }
    
    public static MetricTreeNode getUnPausenotificationsNode(SlackDndInfo slackDndInfo) {
        String endTimeOfDay = SoftwareUtil.getTimeOfDay(SoftwareUtil.getJavaDateFromSeconds(slackDndInfo.snooze_endtime));
        return new MetricTreeNode("Turn on notifications (ends at " + endTimeOfDay + ")", "icons8-slack-new-16.png", SWITCH_ON_DND_ID);
    }
    
    public static MetricTreeNode getSetAwayPresenceNode() {
        return new MetricTreeNode("Set presence to away", "icons8-slack-new-16.png", SET_PRESENCE_AWAY_ID);
    }
    
    public static MetricTreeNode getSetActivePresenceNode() {
        return new MetricTreeNode("Set presence to active", "icons8-slack-new-16.png", SET_PRESENCE_ACTIVE_ID);
    }
    
    public static MetricTreeNode buildSlackWorkspacesNode() {
        MetricTreeNode node = new MetricTreeNode("Slack workspaces", null, SLACK_WORKSPACES_NODE_ID);
        List<Integration> workspaces = SlackManager.getSlackWorkspaces();
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
    
    public static MetricTreeNode buildActiveCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.activeCodeTime, mLabels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_TODAY_ID);
    }
    
    public static MetricTreeNode buildCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.codeTime, "rocket.png", CODETIME_TODAY_ID);
    }
    
    public static MetricTreeNode buildLinesAddedTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.linesAdded, mLabels.linesAddedAvgIcon, LINES_ADDED_TODAY_ID);
    }

    public static MetricTreeNode buildLinesRemovedTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.linesRemoved, mLabels.linesRemovedAvgIcon, LINES_DELETED_TODAY_ID);
    }

    public static MetricTreeNode buildKeystrokesTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.keystrokes, mLabels.keystrokesAvgIcon, KEYSTROKES_TODAY_ID);
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
            case SIGN_UP_ID:
                AuthPromptManager.initiateSignupFlow();
                break;
            case LOG_IN_ID:
                AuthPromptManager.initiateLoginFlow();
                break;
            case GOOGLE_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("google", UIInteractionType.click, false);
                break;
            case GITHUB_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("github", UIInteractionType.click, false);
                break;
            case EMAIL_SIGNUP_ID:
                SoftwareSessionManager.launchLogin("email", UIInteractionType.click, false);
                break;
            case LOGGED_IN_ID:
                CodeTimeTreeTopComponent.expandCollapse(LOGGED_IN_ID);
                break;
            case SWITCH_ACCOUNT_ID:
                AuthPromptManager.initiateSwitchAccountFlow();
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
                SlackManager.connectSlackWorkspace(() -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case SWITCH_OFF_DARK_MODE_ID:
            case SWITCH_ON_DARK_MODE_ID:
                AppleScriptManager.toggleDarkMode(() -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case SWITCH_OFF_DND_ID:
                SlackManager.pauseSlackNotifications(() -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case SWITCH_ON_DND_ID:
                SlackManager.enableSlackNotifications(() -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case SET_PRESENCE_ACTIVE_ID:
                SlackManager.toggleSlackPresence("auto", () -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case SET_PRESENCE_AWAY_ID:
                SlackManager.toggleSlackPresence("away", () -> {CodeTimeTreeTopComponent.rebuildTree();});
                break;
            case TOGGLE_DOCK_POSITION_ID:
                AppleScriptManager.toggleDock();
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
