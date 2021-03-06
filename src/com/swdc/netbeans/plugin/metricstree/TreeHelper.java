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
import com.swdc.netbeans.plugin.managers.FlowManager;
import com.swdc.netbeans.plugin.managers.ScreenManager;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.StringUtils;
import org.openide.awt.HtmlBrowser;
import swdc.java.ops.manager.AppleScriptManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.ConfigSettings;
import swdc.java.ops.model.Integration;
import swdc.java.ops.model.MetricLabel;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;
import swdc.java.ops.model.SlackUserProfile;


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
    public static final String TODAY_VS_AVG_ID = "today_vs_average";
    
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
    public static final String AUTOMATIONS_NODE_ID = "flow_mode_automations";
    public static final String FLOW_MODE_SETTINGS_ID = "flow_mode_settings";
    public static final String ENABLE_FLOW_MODE_ID = "enable_flow_mode";
    public static final String PAUSE_FLOW_MODE_ID = "pause_flow_mode";
    public static final String SWITCH_OFF_DARK_MODE_ID = "switch_off_dark_mode";
    public static final String SWITCH_ON_DARK_MODE_ID = "switch_on_dark_mode";
    public static final String ENTER_FULL_SCREEN_MODE_ID = "enter_full_screen_mode";
    public static final String EXIT_FULL_SCREEN_MODE_ID = "exit_full_screen_mode";
    public static final String TOGGLE_DOCK_POSITION_ID = "toggle_dock_position";
    public static final String SWITCH_OFF_DND_ID = "switch_off_dnd";
    public static final String SWITCH_ON_DND_ID = "switch_on_dnd";
    public static final String CONNECT_SLACK_ID = "connect_slack";
    public static final String ADD_WORKSPACE_ID = "add_workspace";
    public static final String SET_PRESENCE_AWAY_ID = "set_presence_away";
    public static final String SET_PRESENCE_ACTIVE_ID = "set_presence_active";
    public static final String SET_SLACK_STATUS_ID = "set_slack_status";
    
    public static final String PAUSE_NOTIFICATIONS_SETTING_ID = "pause_notifications_setting";
    public static final String SLACK_AWAY_STATUS_SETTING_ID = "slack_away_status_setting";
    public static final String SLACK_AWAY_STATUS_TEXT_SETTING_ID = "slack_away_status_text_setting";
    public static final String SCREEN_MODE_SETTING_ID = "screen_mode_setting";

    private static final SimpleDateFormat formatDay = new SimpleDateFormat("EEE");
    
    public static List<MetricTreeNode> buildSignupNodes() {
        List<MetricTreeNode> list = new ArrayList<>();
        String name = FileUtilManager.getItem("name");
        if (name == null || name.equals("")) {
            list.add(new MetricTreeNode("Sign up", "paw.png", SIGN_UP_ID));
            list.add(new MetricTreeNode("Log in", "paw.png", LOG_IN_ID));
        } else {
            list.add(buildLoggedInNode());
            list.add(buildSwitchAccountNode());
        }
        return list;
    }
    
    public static MetricTreeNode buildLoggedInNode() {
        String authType = FileUtilManager.getItem("authType");
        String name = FileUtilManager.getItem("name");
        String iconName = "email.png";
        if ("google".equals(authType)) {
            iconName = "google.png";
        } else if ("github".equals(authType)) {
            iconName = "github.png";
        }
        
        return new MetricTreeNode(name, iconName, LOGGED_IN_ID);
    }
    
    public static MetricTreeNode buildSwitchAccountNode() {
        return new MetricTreeNode("Switch account", "paw.png", SWITCH_ACCOUNT_ID);
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
        return new MetricTreeNode("More data at Software.com", "paw.png", ADVANCED_METRICS_ID);
    }
    
    public static List<MetricTreeNode> buildTreeFlowNodes() {
        List<MetricTreeNode> list = new ArrayList<>();

        list.add(getFlowModeNode());

        list.add(getFlowModeSettingNodes());

        MetricTreeNode automationsFolder = new MetricTreeNode("Automations", null, AUTOMATIONS_NODE_ID);
        list.add(automationsFolder);

        // full screen toggle node
        automationsFolder.add(getToggleFullScreenNode());

        // change slack status
        automationsFolder.add(getSetSlackStatusNode());

        SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();

        // snooze node
        if (slackDndInfo.snooze_enabled) {
            automationsFolder.add(getUnPausenotificationsNode(slackDndInfo));
        } else {
            automationsFolder.add(getPauseNotificationsNode());
        }
        // presence toggle
        SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();
        if (slackUserPresence != null && slackUserPresence.presence.equals("active")) {
            automationsFolder.add(getSetAwayPresenceNode());
        } else {
            automationsFolder.add(getSetActivePresenceNode());
        }

        if (UtilManager.isMac()) {
            if (AppleScriptManager.isDarkMode()) {
                automationsFolder.add(getSwitchOffDarkModeNode());
            } else {
                automationsFolder.add(getSwitchOnDarkModeNode());
            }

            automationsFolder.add(new MetricTreeNode("Toggle dock position", "position.png", TOGGLE_DOCK_POSITION_ID));
        }

        return list;
    }
    
    public static MetricTreeNode getFlowModeSettingNodes() {
        MetricTreeNode settingsFolder = new MetricTreeNode("Settings", null, FLOW_MODE_SETTINGS_ID);

        ConfigSettings settings = ConfigManager.getConfigSettings();
        String notificationsVal = settings.pauseSlackNotifications ? "on" : "off";
        // Pause notifications
        settingsFolder.add(new MetricTreeNode("Pause notifications (" + notificationsVal + ")", "profile.png", PAUSE_NOTIFICATIONS_SETTING_ID));

        String awayStatusVal = settings.slackAwayStatus ? "on" : "off";
        // Slack away status
        settingsFolder.add(new MetricTreeNode("Slack away status (" + awayStatusVal + ")", "profile.png", SLACK_AWAY_STATUS_SETTING_ID));

        String awayStatusTextVal = (StringUtils.isNotBlank(settings.slackAwayStatusText)) ? " (" + settings.slackAwayStatusText + ")" : "";
        // Slackaway status text
        settingsFolder.add(new MetricTreeNode("Slack away text" + awayStatusTextVal, "profile.png", SLACK_AWAY_STATUS_TEXT_SETTING_ID));

        // Screen mode
        settingsFolder.add(new MetricTreeNode("Screen mode (" + settings.screenMode + ")", "profile.png", SCREEN_MODE_SETTING_ID));

        return settingsFolder;
    }
    
    public static MetricTreeNode getFlowModeNode() {
        String label = "Enable flow mode";
        String icon = "dot-outlined.png";
        String id = ENABLE_FLOW_MODE_ID;
        if (FlowManager.isInFlowMode()) {
            label = "Pause Flow Mode";
            icon = "dot.png";
            id = PAUSE_FLOW_MODE_ID;
        }
        return new MetricTreeNode(label, icon, id);
    }
    
    public static MetricTreeNode getToggleFullScreenNode() {
        String label = "Enter full screen";
        String icon = "expand.png";
        String id = ENTER_FULL_SCREEN_MODE_ID;
        if (ScreenManager.isFullScreen()) {
            label = "Exit full screen";
            icon = "compress.png";
            id = EXIT_FULL_SCREEN_MODE_ID;
        }
        return new MetricTreeNode(label, icon, id);
    }
    
    public static MetricTreeNode getSetSlackStatusNode() {
        SlackUserProfile userProfile = SlackManager.getSlackStatus();
        String status = (userProfile != null && StringUtils.isNotBlank(userProfile.status_text)) ? " (" + userProfile.status_text + ")" : "";
        return new MetricTreeNode("Update profile status" + status, "profile.png", SET_SLACK_STATUS_ID);
    }
    
    public static MetricTreeNode getSwitchOffDarkModeNode() {
        return new MetricTreeNode("Turn off dark mode", "adjust.png", SWITCH_OFF_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getSwitchOnDarkModeNode() {
        return new MetricTreeNode("Turn on dark mode", "adjust.png", SWITCH_ON_DARK_MODE_ID);
    }
    
    public static MetricTreeNode getPauseNotificationsNode() {
        return new MetricTreeNode("Pause notifications", "notifications-off.png", SWITCH_OFF_DND_ID);
    }
    
    public static MetricTreeNode getUnPausenotificationsNode(SlackDndInfo slackDndInfo) {
        String endTimeOfDay = SoftwareUtil.getTimeOfDay(SoftwareUtil.getJavaDateFromSeconds(slackDndInfo.snooze_endtime));
        return new MetricTreeNode("Turn on notifications (ends at " + endTimeOfDay + ")", "notifications-on.png", SWITCH_ON_DND_ID);
    }
    
    public static MetricTreeNode getSetAwayPresenceNode() {
        return new MetricTreeNode("Set presence to away", "presence.png", SET_PRESENCE_AWAY_ID);
    }
    
    public static MetricTreeNode getSetActivePresenceNode() {
        return new MetricTreeNode("Set presence to active", "presence.png", SET_PRESENCE_ACTIVE_ID);
    }
    
    public static MetricTreeNode buildSlackWorkspacesNode() {
        MetricTreeNode node = new MetricTreeNode("Slack workspaces", null, SLACK_WORKSPACES_NODE_ID);
        List<Integration> workspaces = SlackManager.getSlackWorkspaces();
        workspaces.forEach(workspace -> {
            node.add(new MetricTreeNode(workspace.team_domain, "slack.png", workspace.authId));
        });
        // add the add new workspace button
        node.add(getAddSlackWorkspaceNode());
        return node;
    }
    
    public static MetricTreeNode getAddSlackWorkspaceNode() {
        return new MetricTreeNode("Add workspace", "add.png", ADD_WORKSPACE_ID);
    }
    
    public static MetricTreeNode buildTodayVsAverageNode() {
        String refClass = FileUtilManager.getItem("reference-class", "user");
        String labelExt = refClass.equals("user") ? " your daily average" : " the global daily average";
        return new MetricTreeNode("Today vs." + labelExt, "today.png", TODAY_VS_AVG_ID);
    }
    
    public static MetricTreeNode buildActiveCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.activeCodeTime, mLabels.activeCodeTimeAvgIcon, ACTIVE_CODETIME_TODAY_ID);
    }
    
    public static MetricTreeNode buildCodeTimeTree(MetricLabel mLabels) {
        return new MetricTreeNode(mLabels.codeTime, mLabels.codeTimeIcon, CODETIME_TODAY_ID);
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
                SwingUtilities.invokeLater(() -> {
                    SlackManager.connectSlackWorkspace(() -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SWITCH_OFF_DARK_MODE_ID:
            case SWITCH_ON_DARK_MODE_ID:
                SwingUtilities.invokeLater(() -> {
                    AppleScriptManager.toggleDarkMode(() -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SWITCH_OFF_DND_ID:
                SwingUtilities.invokeLater(() -> {
                    SlackManager.pauseSlackNotifications(() -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SWITCH_ON_DND_ID:
                SwingUtilities.invokeLater(() -> {
                    SlackManager.enableSlackNotifications(() -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SET_PRESENCE_ACTIVE_ID:
                SwingUtilities.invokeLater(() -> {
                    SlackManager.toggleSlackPresence("auto", () -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SET_PRESENCE_AWAY_ID:
                SwingUtilities.invokeLater(() -> {
                    SlackManager.toggleSlackPresence("away", () -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case TOGGLE_DOCK_POSITION_ID:
                SwingUtilities.invokeLater(() -> {
                    AppleScriptManager.toggleDock();
                });
                break;
            case SET_SLACK_STATUS_ID:
                SwingUtilities.invokeLater(() -> {
                    SlackManager.setProfileStatus(() -> {CodeTimeTreeTopComponent.refresh();});
                });
                break;
            case SLACK_WORKSPACES_NODE_ID:
            case AUTOMATIONS_NODE_ID:
            case FLOW_MODE_SETTINGS_ID:
                // expand/collapse
                CodeTimeTreeTopComponent.expandCollapse(node.getId());
                break;
            case TODAY_VS_AVG_ID:
                // refresh and change the reference class
                String refClass = FileUtilManager.getItem("reference-class", "user");
                if (refClass.equals("user")) {
                    refClass = "global";
                } else {
                    refClass = "user";
                }
                FileUtilManager.setItem("reference-class", refClass);
                CodeTimeTreeTopComponent.refresh();
                break;
            case ENTER_FULL_SCREEN_MODE_ID:
                SwingUtilities.invokeLater(() -> {
                    ScreenManager.enterFullScreen();
                });
                break;
            case EXIT_FULL_SCREEN_MODE_ID:
                SwingUtilities.invokeLater(() -> {
                    ScreenManager.exitFullScreen();
                });
                break;
            case ENABLE_FLOW_MODE_ID:
                FlowManager.initiateFlow();
                break;
            case PAUSE_FLOW_MODE_ID:
                FlowManager.pauseFlowInitiate();
                break;
            case SCREEN_MODE_SETTING_ID:
                ConfigManager.modifyScreenMode(() -> {CodeTimeTreeTopComponent.refresh();});
                break;
            case PAUSE_NOTIFICATIONS_SETTING_ID:
                ConfigManager.modifyPauseNotifications(() -> {CodeTimeTreeTopComponent.refresh();});
                break;
            case SLACK_AWAY_STATUS_SETTING_ID:
                ConfigManager.modifySlackAwayStatus(() -> {CodeTimeTreeTopComponent.refresh();});
                break;
            case SLACK_AWAY_STATUS_TEXT_SETTING_ID:
                ConfigManager.modifySlackStatusText(() -> {CodeTimeTreeTopComponent.refresh();});
                break;
        }
    }
    
    public static JSeparator getSeparator() {
        JSeparator separator = new JSeparator();
        separator.setAlignmentY(0.0f);
        separator.setForeground(new Color(58, 86, 187));
        return separator;
    }
    
}
