/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.http.client.methods.HttpGet;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.awt.HtmlBrowser;

public class SoftwareSessionManager {

    private static SoftwareSessionManager instance = null;
    public static final Logger log = Logger.getLogger("SoftwareCoSessionManager");
    private static long lastAppAvailableCheck = 0;
    public static boolean establishingUser = false;

    public static SoftwareSessionManager getInstance() {
        if (instance == null) {
            instance = new SoftwareSessionManager();
        }
        return instance;
    }

    public static boolean softwareSessionFileExists() {
        // don't auto create the file
        String file = getSoftwareSessionFile(false);
        // check if it exists
        File f = new File(file);
        return f.exists();
    }

    public static String getCodeTimeDashboardFile() {
        String dashboardFile = getSoftwareDir(true);
        if (SoftwareUtil.isWindows()) {
            dashboardFile += "\\CodeTime.txt";
        } else {
            dashboardFile += "/CodeTime.txt";
        }
        return dashboardFile;
    }

    public static String getReadmeFile() {
        String file = getSoftwareDir(true);
        if (SoftwareUtil.isWindows()) {
            file += "\\jetbrainsCt_README.txt";
        } else {
            file += "/jetbrainsCt_README.txt";
        }
        return file;
    }

    public static String getSoftwareDir(boolean autoCreate) {
        String softwareDataDir = SoftwareUtil.getUserHomeDir();
        if (SoftwareUtil.isWindows()) {
            softwareDataDir += "\\.software";
        } else {
            softwareDataDir += "/.software";
        }

        File f = new File(softwareDataDir);
        if (!f.exists()) {
            // make the directory
            f.mkdirs();
        }

        return softwareDataDir;
    }

    public static String getSummaryInfoFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (SoftwareUtil.isWindows()) {
            file += "\\SummaryInfo.txt";
        } else {
            file += "/SummaryInfo.txt";
        }
        return file;
    }

    public static String getSoftwareSessionFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (SoftwareUtil.isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    public synchronized static boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        // 5 min threshold
        boolean pastThreshold = nowInSec - lastAppAvailableCheck > (60 * 5);
        if (pastThreshold) {
            SoftwareResponse resp = SoftwareUtil.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
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
        boolean establishedUser = SoftwareUtil.getUserLoginState();

        if (!establishedUser && retryCount > 0) {
            final int newRetryCount = retryCount - 1;

            final Runnable service = () -> lazilyFetchUserStatus(newRetryCount);
            AsyncManager.getInstance().executeOnceInSeconds(service, 8);
        } else {
            establishingUser = false;
            // prompt they've completed the setup

            String infoMsg = "Successfully logged onto Code Time";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Code Time Setup Complete", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            FileManager.sendOfflineData();

        }
    }

    public static void launchLogin(String loginType, UIInteractionType interactionType) {

        String jwt = FileManager.getItem("jwt");

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
            url = SoftwareUtil.LAUNCH_URL + "/email-signup?token=" + jwt + "&plugin=codetime&auth=software";
        } else if (loginType.equals("google")) {
            url = SoftwareUtil.API_ENDPOINT + "/auth/google?token=" + jwt + "&plugin=codetime&redirect=" + SoftwareUtil.LAUNCH_URL;
        } else if (loginType.equals("github")) {
            element_name = "ct_sign_up_github_btn";
            cta_text = "Sign up with GitHub";
            icon_name = "github";
            url = SoftwareUtil.API_ENDPOINT + "/auth/github?token=" + jwt + "&plugin=codetime&redirect=" + SoftwareUtil.LAUNCH_URL;
        }

        FileManager.setItem("authType", loginType);
        
        try {
            URL launchUrl = new URL(url);
            HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        if (!establishingUser) {
            establishingUser = true;
            // max of 5.3 minutes
            final Runnable service = () -> lazilyFetchUserStatus(40);
            AsyncManager.getInstance().executeOnceInSeconds(service, 8);
        }

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = element_name;
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = icon_color;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = icon_name;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }

    public static void launchWebDashboard(UIInteractionType interactionType) {
        String url = SoftwareUtil.LAUNCH_URL + "/login";
        try {
            URL launchUrl = new URL(url);
            HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
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
            log.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
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
