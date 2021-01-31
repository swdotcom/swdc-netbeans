/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;


import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.model.ConfigSettings;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;
import swdc.java.ops.model.SlackUserProfile;

public class FlowManager {
    public static boolean enabledFlow = false;

    private static boolean enablingFlow = false;
    private static boolean useSlackSettings = true;

    public static void checkToDisableFlow() {
        ScreenManager.isFullScreen();
        if (!enabledFlow || enablingFlow) {
            return;
        } else if (!useSlackSettings && !isScreenStateInFlow()) {
            // slack isn't connected but the screen state changed out of flow
            pauseFlowInitiate();
            return;
        }

        if (enabledFlow && !isInFlowMode()) {
            // disable it
            pauseFlowInitiate();
        }
    }

    public static void initiateFlow() {
        boolean isRegistered = SlackManager.checkRegistration(false, null);
        if (!isRegistered) {
            // show the flow mode prompt
            SlackManager.showModalSignupPrompt("To use Flow Mode, please first sign up or login.", () -> { CodeTimeTreeTopComponent.refresh();});
            return;
        }
        enablingFlow = true;

        ConfigSettings configSettings = ConfigManager.getConfigSettings();

        // set slack status to away
        if (configSettings.slackAwayStatus) {
            SlackManager.setSlackPresence("away");
        }

        // set the status text to what the user set in the settings
        boolean clearStatusText = StringUtils.isBlank(configSettings.slackAwayStatusText) ? true : false;
        SlackManager.updateSlackStatusText(configSettings.slackAwayStatusText, ":large_purple_circle:", clearStatusText);


        // pause slack notifications
        if (configSettings.pauseSlackNotifications) {
            SlackManager.pauseSlackNotifications();
        }

        if (configSettings.screenMode.contains("Full Screen")) {
            ScreenManager.enterFullScreen();
        } else {
            ScreenManager.exitFullScreen();
        }

        SlackManager.clearSlackCache();

        SwingUtilities.invokeLater(() -> {
            CodeTimeTreeTopComponent.refresh();
        });

        enabledFlow = true;
        enablingFlow = false;
    }

    public static void pauseFlowInitiate() {
        ConfigSettings configSettings = ConfigManager.getConfigSettings();

        SlackManager.enableSlackNotifications();
        SlackManager.setSlackPresence("auto");
        SlackManager.updateSlackStatusText("", "", true);
        ScreenManager.exitFullScreen();

        SlackManager.clearSlackCache();

        SwingUtilities.invokeLater(() -> {
            CodeTimeTreeTopComponent.refresh();
        });

        enabledFlow = false;
        enablingFlow = false;
    }

    public static boolean isInFlowMode() {
        if (enablingFlow) {
            return true;
        } else if (!enabledFlow) {
            return false;
        }

        ConfigSettings settings = ConfigManager.getConfigSettings();

        useSlackSettings = SlackManager.hasSlackWorkspaces();

        boolean screenInFlowState = isScreenStateInFlow();

        SlackUserProfile slackUserProfile = SlackManager.getSlackStatus();
        SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();
        SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();

        boolean pauseSlackNotificationsInFlowState = false;
        if (!useSlackSettings) {
            pauseSlackNotificationsInFlowState = true;
        } else if (settings.pauseSlackNotifications && slackDndInfo.snooze_enabled) {
            pauseSlackNotificationsInFlowState = true;
        } else if (!settings.pauseSlackNotifications && !slackDndInfo.snooze_enabled) {
            pauseSlackNotificationsInFlowState = true;
        }

        // determine if the slack away status text is in flow
        boolean slackAwayStatusMsgInFlowState = false;
        if (!useSlackSettings) {
            slackAwayStatusMsgInFlowState = true;
        } else if (settings.slackAwayStatusText.equals(slackUserProfile.status_text)) {
            slackAwayStatusMsgInFlowState = true;
        }

        boolean slackAwayPresenceInFlowState = false;
        if (!useSlackSettings) {
            slackAwayPresenceInFlowState = true;
        } else if (settings.slackAwayStatus && slackUserPresence.presence.equals("")) {
            slackAwayPresenceInFlowState = true;
        } else if (!settings.slackAwayStatus && slackUserPresence.presence.equals("active")) {
            slackAwayPresenceInFlowState = true;
        }

        return screenInFlowState && pauseSlackNotificationsInFlowState && slackAwayStatusMsgInFlowState && slackAwayPresenceInFlowState;
    }

    public static boolean isScreenStateInFlow() {
        ConfigSettings settings = ConfigManager.getConfigSettings();
        boolean screenInFlowState = false;
        if (settings.screenMode.contains("Full Screen") && ScreenManager.isFullScreen()) {
            screenInFlowState = true;
        } else if (settings.screenMode.contains("None") && !ScreenManager.isFullScreen()) {
            screenInFlowState = true;
        }

        return screenInFlowState;
    }
}