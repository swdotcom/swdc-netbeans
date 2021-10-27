/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;


import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import swdc.java.ops.http.FlowModeClient;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;

public class FlowManager {
    public static boolean enabledFlow = false;

    public static void initFlowStatus() {
        boolean originalState = enabledFlow;
        enabledFlow = FlowModeClient.isFlowModeOn();
        if (originalState != enabledFlow) {
            updateFlowStateDisplay();
        }
    }

    public static void toggleFlowMode(boolean automated) {
        if (!enabledFlow) {
            enterFlowMode(automated);
        } else {
            exitFlowMode();
        }
    }

    public static void enterFlowMode(boolean automated) {
        if (enabledFlow) {
            updateFlowStateDisplay();
            return;
        }

        boolean isRegistered = AccountManager.checkRegistration(false, null);
        if (!isRegistered) {
            // show the flow mode prompt
            AccountManager.showModalSignupPrompt("To use Flow Mode, please first sign up or login.", () -> {
            	CodeTimeTreeTopComponent.refresh();
            });
            return;
        }

        boolean eclipse_CtskipSlackConnect = FileUtilManager.getBooleanItem("eclipse_CtskipSlackConnect");
        boolean workspaces = SlackManager.hasSlackWorkspaces();
        if (!workspaces && !eclipse_CtskipSlackConnect) {
            String msg = "Connect a Slack workspace to pause\nnotifications and update your status?";

            Object[] options = {"Connect", "Skip"};
            Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", FlowManager.class.getClassLoader());

            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showOptionDialog(
                        null, msg, "Slack connect", JOptionPane.OK_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon, options, options[0]);

                if (choice == 0) {
                    SlackManager.connectSlackWorkspace(() -> {
                    	   CodeTimeTreeTopComponent.refresh();
                    });
                } else {
                    FileUtilManager.setBooleanItem("eclipse_CtskipSlackConnect", true);
                    FlowManager.enterFlowMode(automated);
                }
            });
            return;
        }

        FlowModeClient.enterFlowMode(automated);

        enabledFlow = true;

        updateFlowStateDisplay();
    }

    public static void exitFlowMode() {
        if (!enabledFlow) {
            updateFlowStateDisplay();
            return;
        }

        FlowModeClient.exitFlowMode();

        enabledFlow = false;

        updateFlowStateDisplay();
    }

    private static void updateFlowStateDisplay() {
    	SwingUtilities.invokeLater(() -> {
    		// at least update the status bar
            AsyncManager.getInstance().executeOnceInSeconds(() -> {
            	CodeTimeTreeTopComponent.refresh();
            }, 2);
            SessionDataManager.updateFileSummaryAndStatsBar(null);
    	});
    }

    public static boolean isFlowModeEnabled() {
        return enabledFlow;
    }
}