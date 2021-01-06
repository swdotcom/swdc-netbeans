/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.UserState;

public class SoftwareSessionManager {

    private static SoftwareSessionManager instance = null;
    public static final Logger LOG = Logger.getLogger("SoftwareCoSessionManager");
    private static long lastAppAvailableCheck = 0;

    public static SoftwareSessionManager getInstance() {
        if (instance == null) {
            instance = new SoftwareSessionManager();
        }
        return instance;
    }

    public static String getReadmeFile() {
        String file = FileUtilManager.getSoftwareDir(true);
        if (UtilManager.isWindows()) {
            file += "\\netbeansCt_README.txt";
        } else {
            file += "/netbeansCt_README.txt";
        }
        return file;
    }

    public synchronized static boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        // 5 min threshold
        boolean pastThreshold = nowInSec - lastAppAvailableCheck > (60 * 5);
        if (pastThreshold) {
            ClientResponse resp = OpsHttpClient.softwareGet("/ping", null);
            SoftwareUtil.updateServerStatus(resp.isOk());
            lastAppAvailableCheck = nowInSec;
        }
        return SoftwareUtil.isAppAvailable();
    }

    private Project getCurrentProject() {
        Set<Project> projects = ProjectManager.getDefault().getModifiedProjects();
        if (projects != null && projects.size() > 0) {
            return projects.iterator().next();
        }
        return null;
    }

    public void statusBarClickHandler() {
        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = "ct_status_bar_metrics_btn";
        elementEntity.element_location = "ct_status_bar";
        elementEntity.color = null;
        elementEntity.cta_text = "status bar metrics";
        elementEntity.icon_name = "clock";
        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
        CodeTimeTreeTopComponent.openTree();
    }

    protected static void lazilyFetchUserStatus(int retryCount) {
        UserState loginState = AccountManager.getUserLoginState(false);

        if (!loginState.loggedIn) {
            if (retryCount > 0) {
                final int newRetryCount = retryCount - 1;

                final Runnable service = () -> lazilyFetchUserStatus(newRetryCount);
                AsyncManager.getInstance().executeOnceInSeconds(service, 10);
            } else {
                // clear the auth callback state
                FileUtilManager.setBooleanItem("switching_account", false);
                FileUtilManager.setAuthCallbackState(null);
            }
        } else {
            // pull in any integrations
            SlackManager.getSlackAuth(loginState.user);
            
            SessionDataManager.clearSessionSummaryData();
            TimeDataManager.clearTimeDataSummary();
            
            // clear the auth callback state
            FileUtilManager.setBooleanItem("switching_account", false);
            FileUtilManager.setAuthCallbackState(null);

            // prompt they've completed the setup
            String infoMsg = "Successfully logged onto Code Time";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Code Time Setup Complete", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            
            WallClockManager.getInstance().refreshSessionDataAndTree();
            
        }
    }

    public static void launchLogin(String loginType, UIInteractionType interactionType, boolean switching_account) {
        try {
            String auth_callback_state = FileUtilManager.getAuthCallbackState(true);
            FileUtilManager.setBooleanItem("switching_account", switching_account);

            String plugin_uuid = FileUtilManager.getPluginUuid();

            JsonObject obj = new JsonObject();
            obj.addProperty("plugin", "codetime");
            obj.addProperty("plugin_uuid", plugin_uuid);
            obj.addProperty("pluginVersion", SoftwareUtil.getVersion());
            obj.addProperty("plugin_id", SoftwareUtil.PLUGIN_ID);
            obj.addProperty("auth_callback_state", auth_callback_state);
            obj.addProperty("redirect", SoftwareUtil.LAUNCH_URL);

            String url = "";
            String element_name = "ct_sign_up_google_btn";
            String icon_name = "google";
            String cta_text = "Sign up with Google";
            String icon_color = null;
            if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
                element_name = "ct_sign_up_email_btn";
                cta_text = "Sign up with email";
                icon_name = "envelope";
                icon_color = "gray";
                url = SoftwareUtil.LAUNCH_URL + "/email-signup";
            } else if (loginType.equals("google")) {
                url = SoftwareUtil.API_ENDPOINT + "/auth/google";
            } else if (loginType.equals("github")) {
                element_name = "ct_sign_up_github_btn";
                cta_text = "Sign up with GitHub";
                icon_name = "github";
                url = SoftwareUtil.API_ENDPOINT + "/auth/github";
            }

            url += SoftwareUtil.buildQueryString(obj);

            FileUtilManager.setItem("authType", loginType);
        
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
            
            // max of 5.3 minutes
            final Runnable service = () -> lazilyFetchUserStatus(40);
            AsyncManager.getInstance().executeOnceInSeconds(service, 8);

            UIElementEntity elementEntity = new UIElementEntity();
            elementEntity.element_name = element_name;
            elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
            elementEntity.color = icon_color;
            elementEntity.cta_text = cta_text;
            elementEntity.icon_name = icon_name;
            EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}", e.getMessage());
        }
    }

    public static void launchWebDashboard(UIInteractionType interactionType) {
        if (StringUtils.isBlank(FileUtilManager.getItem("name"))) {
            SwingUtilities.invokeLater(() -> {
                String msg = "Sign up or log in to see more data visualizations.";

                Object[] options = {"Sign up"};
                int choice = JOptionPane.showOptionDialog(
                        null, msg, "Sign up", JOptionPane.OK_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == 0) {
                    AuthPromptManager.initiateSignupFlow();
                }
            });
            return;
        }
        String url = SoftwareUtil.LAUNCH_URL + "/login";
        try {
            URL launchUrl = new URL(url);
            HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_web_metrics_btn" : "ct_web_metrics_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "gray" : null;
        elementEntity.cta_text = "See advanced metrics";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "paw" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }
    
    public static void submitFeedback(UIInteractionType interactionType) {
        String url = "mailto:cody@software.com";
        try {
            URL launchUrl = new URL(url);
            HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_submit_feedback_btn" : "ct_submit_feedback_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = null;
        elementEntity.cta_text = "Submit feedback";
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "text-bubble" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }
}
