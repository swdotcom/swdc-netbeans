/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.model.ConfigSettings;
import swdc.java.ops.model.SlackDndInfo;
import swdc.java.ops.model.SlackUserPresence;
import swdc.java.ops.model.SlackUserProfile;

public class FlowManager {
    private static boolean enablingFlow = false;
    private static boolean enabledFlow = false;
    private static boolean useSlackSettings = true;

    public static void checkToDisableFlow() {
        if (!enabledFlow || enablingFlow) {
            return;
        } else if (!useSlackSettings && !isScreenStateInFlow()) {
            // slack isn't connected but the screen state changed out of flow
            pauseFlowInitiate();
            return;
        }

        SlackUserProfile slackUserProfile = SlackManager.getSlackStatus();
        SlackDndInfo slackDndInfo = SlackManager.getSlackDnDInfo();
        SlackUserPresence slackUserPresence = SlackManager.getSlackUserPresence();

        //const [slackStatus, slackPresence, slackDnDInfo] = await Promise.all([getSlackStatus(), getSlackPresence(), getSlackDnDInfo()]);
        if (enabledFlow && !isInFlowMode(slackUserProfile, slackUserPresence, slackDndInfo)) {
            // disable it
            pauseFlowInitiate();
        }
    }

    public static void initiateFlow(Runnable screenModeCallback) {
        boolean isRegistered = SlackManager.checkRegistration(false, null);
        if (!isRegistered) {
            // show the flow mode prompt
            SlackManager.showModalSignupPrompt("To use Flow Mode, please first sign up or login.", null);
            return;
        }
        enablingFlow = true;

        ConfigSettings configSettings = FileUtilManager.getConfigSettings();

        // set slack status to away
        if (configSettings.slackAwayStatus) {
            SlackManager.toggleSlackPresence("away", null);
        }

        // set the status text to what the user set in the settings
        boolean clearStatusText = StringUtils.isBlank(configSettings.slackAwayStatusText) ? true : false;
        SlackManager.updateSlackStatusText(configSettings.slackAwayStatusText, ":large_purple_circle:", clearStatusText, null);


        // pause slack notifications
        if (configSettings.pauseSlackNotifications) {
            SlackManager.pauseSlackNotifications(null);
        }

        if (configSettings.screenMode.contains("Full Screen")) {
            ScreenManager.enterFullScreenMode();
        } else {
            ScreenManager.exitFullScreenMode();
        }

        SlackManager.clearSlackCache();

        CodeTimeTreeTopComponent.refresh();

        enabledFlow = true;
        enablingFlow = false;
    }

    public static void pauseFlowInitiate() {
        ConfigSettings configSettings = FileUtilManager.getConfigSettings();

        SlackManager.enableSlackNotifications(null);
        SlackManager.toggleSlackPresence("auto", null);
        SlackManager.updateSlackStatusText("", "", true, null);
        ScreenManager.exitFullScreenMode();

        SlackManager.clearSlackCache();

        CodeTimeTreeTopComponent.refresh();

        enabledFlow = false;
        enablingFlow = false;
    }

    public static boolean isInFlowMode(SlackUserProfile slackUserProfile, SlackUserPresence slackUserPresence, SlackDndInfo slackDndInfo) {
        return false;
    }

    public static boolean isScreenStateInFlow() {
        return false;
    }
}