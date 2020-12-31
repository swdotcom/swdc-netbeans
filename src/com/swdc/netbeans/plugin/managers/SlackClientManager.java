/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import com.swdc.netbeans.plugin.models.Integration;
import com.swdc.netbeans.plugin.models.SlackDndInfo;
import com.swdc.netbeans.plugin.models.SlackUserPresence;
import com.swdc.netbeans.plugin.models.SlackUserProfile;
import com.swdc.netbeans.plugin.models.SoftwareUser;
import com.swdc.netbeans.plugin.models.UserLoginState;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.openide.awt.HtmlBrowser;
import org.openide.util.Exceptions;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 *
 * @author xavierluiz
 */
public class SlackClientManager {
    public static final Logger LOG = Logger.getLogger("SlackClientManager");

    private static SlackUserProfile current_slack_profile = null;
    private static SlackUserPresence current_slack_presence = null;
  

    // -------------------------------------------
    // - public methods
    // -------------------------------------------
    // get saved slack integrations
    public static List<Integration> getSlackWorkspaces() {
        List<Integration> slackIntegrations = new ArrayList<>();
        for (Integration integration : FileManager.getIntegrations()) {
            if (integration.name.toLowerCase().equals("slack")) {
                slackIntegrations.add(integration);
            }
        }
        return slackIntegrations;
    }

    public static Integration getSlackWorkspace(String authId) {
        for (Integration integration : FileManager.getIntegrations()) {
            if (integration.authId.equals(authId)) {
                return integration;
            }
        }
        return null;
    }

    public static boolean hasSlackWorkspaces() {
        return (getSlackWorkspaces().size() > 0);
    }
    
    public static void connectSlackWorkspace() {
        boolean registered = checkRegistration(true);
        if (!registered) {
            return;
        }
        
        JsonObject obj = new JsonObject();
        obj.addProperty("plugin", "codetime");
        obj.addProperty("plugin_uuid", FileManager.getPluginUuid());
        obj.addProperty("pluginVersion", SoftwareUtil.getVersion());
        obj.addProperty("plugin_id", SoftwareUtil.PLUGIN_ID);
        obj.addProperty("auth_callback_state", FileManager.getAuthCallbackState());
        obj.addProperty("integrate", "slack");
        
        String url = SoftwareUtil.API_ENDPOINT + "/auth/slack" + SoftwareUtil.buildQueryString(obj);
        
        try {
            URL launchUrl = new URL(url);
            HtmlBrowser.URLDisplayer.getDefault().showURL(launchUrl);
            
            final Runnable service = () -> refetchSlackConnectStatusLazily(40);
            AsyncManager.getInstance().executeOnceInSeconds(service, 15);
            
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static void disconnectSlackAuth(String authId) {
         String msg = "Are you sure you would like to disconnect this Slack workspace";

        Object[] options = {"Disconnect"};
        int choice = JOptionPane.showOptionDialog(
                null, msg, "Disconnect", JOptionPane.YES_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) {
            JsonObject payload = new JsonObject();
            payload.addProperty("authId", authId);
            String api = "/auth/slack/disconnect";
            SoftwareResponse resp = SoftwareUtil.makeApiCall(api, HttpPut.METHOD_NAME, payload.toString());
            FileManager.removeSlackIntegration(authId);
            
            CodeTimeTreeTopComponent.rebuildTree();
        }
    }
    
    public static void pauseSlackNotifications() {
        boolean registered = checkRegistration(true);
        if (!registered) {
            return;
        }
        
        boolean updatedSnooze = false;
        List<Integration> integrations = getSlackWorkspaces();
        for (Integration integration : integrations) {
            SoftwareResponse resp = slackGet("/dnd.setSnooze?num_minutes=120", integration.access_token);
            if (resp.isOk()) {
                updatedSnooze = true;
            }
        }
        if (updatedSnooze) {
            // prompt they've completed the setup
            String infoMsg = "Slack notifications are paused for 2 hours";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Slack notifications", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            
            CodeTimeTreeTopComponent.rebuildTree();
        }
    }
    
    public static void enableSlackNotifications() {
        boolean registered = checkRegistration(true);
        if (!registered) {
            return;
        }
        
        boolean updatedSnooze = false;
        List<Integration> integrations = getSlackWorkspaces();
        // response body: {"ok": true,"snooze_enabled": true,"snooze_endtime": 1450373897,"snooze_remaining": 60}
        for (Integration integration : integrations) {
            SoftwareResponse resp = slackPost("/dnd.endSnooze", integration.access_token, null);
            if (resp.isOk()) {
                updatedSnooze = true;
            }
        }
        if (updatedSnooze) {
            // prompt they've completed the setup
            String infoMsg = "Slack notifications enabled";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Slack notifications", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            
            CodeTimeTreeTopComponent.rebuildTree();
        }
    }
    
    public static SlackDndInfo getSlackDnDInfo() {
        List<Integration> integrations = getSlackWorkspaces();
        for (Integration integration : integrations) {
            SlackDndInfo dndInfo = getSlackDnDInfoPerDomain(integration.team_domain);
            if (dndInfo != null) {
                return dndInfo;
            }
        }
        return null;
    }
    
    public static SlackUserPresence getSlackUserPresence() {
        boolean registered = checkRegistration(false);
        if (!registered) {
            return null;
        }
        
        current_slack_presence = null;
        
        List<Integration> integrations = getSlackWorkspaces();
        for (Integration integration : integrations) {
            SlackUserPresence presence = getSlackPresencePerDomain(integration.team_domain);
            if (presence != null) {
                current_slack_presence = presence;
                break;
            }
        }
        return current_slack_presence;
    }
    
    public static Integration getWorkspaceByDomain(String domain) {
        List<Integration> workspaces = getSlackWorkspaces();
        for (Integration workspace : workspaces) {
            if (workspace.team_domain.equals(domain)) {
                return workspace;
            }
        }
        return null;
    }
    
    public static Integration getWorkspaceByAuthId(String authId) {
        List<Integration> workspaces = getSlackWorkspaces();
        for (Integration workspace : workspaces) {
            if (workspace.authId.equals(authId)) {
                return workspace;
            }
        }
        return null;
    }
    
    public static void toggleSlackPresence(String presence) {
        boolean registered = checkRegistration(true);
        if (!registered) {
            return;
        }
        
        boolean updatedPresence = false;
        List<Integration> integrations = getSlackWorkspaces();
        for (Integration integration : integrations) {
            JsonObject obj = new JsonObject();
            obj.addProperty("presence", presence);
            SoftwareResponse resp = slackPost("/users.setPresence", integration.access_token, obj);
            if (resp.isOk()) {
                updatedPresence = true;
            }
        }
        
        if (updatedPresence) {
            // prompt they've completed the setup
            presence = presence.equals("auto") ? "active" : presence;
            String infoMsg = "Slack presence updated to '" + presence + "'";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Slack presence", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            
            CodeTimeTreeTopComponent.rebuildTree();
        }
    }
    
    public static void setProfileStatus() {
        boolean registered = checkRegistration(true);
        if (!registered) {
            return;
        }
        
        String updateInput = getStatusUpdateInput();
        
        if (StringUtils.isBlank(updateInput)) {
            return;
        }
        
        String statusText = "";
        boolean clearStatus = true;
        if (!updateInput.toLowerCase().contains("clear")) {
            clearStatus = false;
            // get the message they would like to use in their slack status
            statusText = getStatusStringInput();
            if (statusText == null) {
                return;
            }
        }
        
        List<Integration> integrations = getSlackWorkspaces();
        for (Integration integration : integrations) {
            JsonObject obj = new JsonObject();
            JsonObject profileObj = new JsonObject();
            profileObj.addProperty("status_text", statusText);
            if (!clearStatus) {
                profileObj.addProperty("status_expiration", 0L);
            }
            obj.add("profile", profileObj);
            slackPost("/users.profile.set", integration.access_token, obj);
        }
    }
    
    public static SlackUserProfile getSlackStatus() {
        boolean registered = checkRegistration(false);
        if (!registered) {
            return null;
        }
        
        current_slack_profile = null;
        
        List<Integration> integrations = getSlackWorkspaces();
        // {ok (bool), profile: {avatar_hash, status_text, status_emoji, status_expiration, real_name,
        //  display_name, real_name_normalized, display_name_normalized, email, image_original, image_..., team}}
        for (Integration integration : integrations) {
            SoftwareResponse resp = slackGet("/users.profile.get", integration.access_token);
            if (resp.isOk()) {
                current_slack_profile = new SlackUserProfile();
                current_slack_profile.updateInfoWithResponse(resp.getJsonObj());
            }
        }
        
        return current_slack_profile;
    }
    
    public static boolean getSlackAuth(SoftwareUser user) {
        List<Integration> existingIntegrations = FileManager.getIntegrations();
        
        if (user == null) {
            // get the software user
            UserLoginState loginState = SoftwareUtil.getUserLoginState(true);
            user = loginState.user;
        }
        
        boolean foundNewIntegration = false;
        if (user != null && user.registered == 1 && !user.integrations.isEmpty()) {
            for (Integration integration : user.integrations) {
                if (integration.name.toLowerCase().equals("slack")) {
                    Integration existing = SlackClientManager.getWorkspaceByAuthId(integration.authId);
                    if (existing == null) {
                        // get the domain info
                        // {ok, user: {name, id}, team: {name, id}}
                        SoftwareResponse resp = slackGet("/users.identity", integration.access_token);
                        if (resp.isOk()) {
                            JsonObject team = JsonManager.getJsonObjectVal(resp.getJsonObj(), "team");
                            integration.team_domain = JsonManager.getStringVal(team, "domain", null);
                            integration.team_name = JsonManager.getStringVal(team, "name", null);
                            
                            // add it, its new
                            existingIntegrations.add(integration);
                            // set the team name and domain
                            foundNewIntegration = true;
                        }
                    }
                }
            }
        }
        
        if (foundNewIntegration) {
            FileManager.syncIntegrations(existingIntegrations);
        }
        
        return foundNewIntegration;
    }
    
    private static void refetchSlackConnectStatusLazily(int tryCountUntilFoundUser) {
        boolean foundNewSlackAuth = getSlackAuth(null);
        if (!foundNewSlackAuth) {
            // try again if the count isnot zero
            if (tryCountUntilFoundUser > 0) {
                final int newCount = tryCountUntilFoundUser - 1;
                final Runnable service = () -> refetchSlackConnectStatusLazily(newCount);
                AsyncManager.getInstance().executeOnceInSeconds(service, 10);
            } else {
                FileManager.setAuthCallbackState(null);
            }
        } else {
            FileManager.setAuthCallbackState(null);
            
            // prompt they've completed the setup
            String infoMsg = "Successfully connected to Slack";
            Object[] options = { "OK" };
            JOptionPane.showOptionDialog(null, infoMsg, "Slack connect", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);
            
            CodeTimeTreeTopComponent.rebuildTree();
        }
    }
    
    private static boolean checkRegistration(boolean showSignup) {
        String name = FileManager.getItem("name");
        if (StringUtils.isBlank(name)) {
            // the user is not registerd
            if (showSignup) {
                String msg = "Connecting Slack requires a registered account. Sign up or log in to continue.";

                Object[] options = {"Sign up"};
                int choice = JOptionPane.showOptionDialog(
                        null, msg, "Code Time", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (choice == 0) {
                    SoftwareUtil.showAuthSelectPrompt(true);
                }
            }
            return false;
        }
        return true;
    }
    
    private static Integration showSlackWorkspaceSelection() {
        String[] options = getWorkspaceOptions();
        if (options != null && options.length > 0) {
            String input = (String) JOptionPane.showInputDialog(null,
                    "Select a Slack workspace",
                    "Workspace",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
            return getWorkspaceByDomain(input);
        }
        return null;
    }
    
    private static String[] getWorkspaceOptions() {
        List<String> options = new ArrayList<String>();
        List<Integration> workspaces = getSlackWorkspaces();
        for (Integration workspace : workspaces) {
            options.add(workspace.team_domain);
        }
        return (String[]) options.toArray();
    }
    
    private static SlackDndInfo getSlackDnDInfoPerDomain(String team_domain) {
        Integration integration = getWorkspaceByDomain(team_domain);
        if (integration != null) {
            SoftwareResponse resp = slackGet("/dnd.info", integration.access_token);
            if (resp.isOk()) {
                SlackDndInfo dndInfo = new SlackDndInfo();
                dndInfo.updateInfoWithResponse(resp.getJsonObj());
                return dndInfo;
            }
        }
        return null;
    }
    
    private static SlackUserPresence getSlackPresencePerDomain(String team_domain) {
        Integration integration = getWorkspaceByDomain(team_domain);
        if (integration != null) {
            
            // {ok (bool), presence (string), online (bool), auto_away (bool), manual_away (bool),
            //  connection_count (int), last_activity (long)}
            SoftwareResponse resp = slackGet("/users.getPresence", integration.access_token);
            if (resp.isOk()) {
                SlackUserPresence userPresence = new SlackUserPresence();
                userPresence.updateInfoWithResponse(resp.getJsonObj());
                return userPresence;
            }
        }
        return null;
    }
    
    private static String getStatusUpdateInput() {
        String[] options = new String[]{ "Clear your status", "Set a new status" };
        String input = (String) JOptionPane.showInputDialog(
                null,
                "Select clear or update to continue",
                "Slack status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options, // Array of choices
                options[0]); // Initial choice
        return input;
    }
    
    private static String getStatusStringInput() {
        String input = (String) JOptionPane.showInputDialog(
                null, "Enter a message to appear in your profile status", "Slack status", JOptionPane.PLAIN_MESSAGE);
        return input;
    }
    

    private static SoftwareResponse slackGet(String api, String access_token) {
        return makeSlackApiCall(HttpGet.METHOD_NAME, api, access_token, null);
    }
    
    private static SoftwareResponse slackPost(String api, String access_token, JsonObject payload) {
        return makeSlackApiCall(HttpPost.METHOD_NAME, api, access_token, payload);
    }
    
    private static SoftwareResponse makeSlackApiCall(String httpMethodName, String api, String access_token, JsonObject payload) { 
        SoftwareResponse softwareResponse = new SoftwareResponse();
        
        SlackHttpManager httpMgr = new SlackHttpManager(api, access_token, httpMethodName, payload);


        Future<HttpResponse> response = SoftwareUtil.EXECUTOR_SERVICE.submit(httpMgr);

        //
        // Handle the Future if it exist
        //
        if (response != null) {
            try {
                HttpResponse httpResponse = response.get();
                if (httpResponse != null) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode < 300) {
                        softwareResponse.setIsOk(true);
                    }
                    HttpEntity entity = httpResponse.getEntity();
                    
                    ContentType contentType = ContentType.getOrDefault(entity);
                    String mimeType = contentType.getMimeType();
                    boolean isPlainText = (mimeType.indexOf("text/plain") == -1) ? false : true;
                    
                    JsonObject jsonObj = null;
                    if (entity != null) {
                        try {
                            String jsonStr = SoftwareUtil.getStringRepresentation(entity, isPlainText);
                            softwareResponse.setJsonStr(jsonStr);
                            
                            if (jsonStr != null && !isPlainText) {
                                LOG.log(Level.INFO, "Sofware.com: API response {0}", jsonStr);
                                Object jsonEl = SoftwareUtil.readAsJsonObject(jsonStr);
                                
                                if (jsonEl instanceof JsonElement) {
                                    try {
                                        JsonElement el = (JsonElement)jsonEl;
                                        if (el.isJsonPrimitive()) {
                                            if (statusCode < 300) {
                                                softwareResponse.setDataMessage(el.getAsString());
                                            } else {
                                                softwareResponse.setErrorMessage(el.getAsString());
                                            }
                                        } else {
                                            jsonObj = ((JsonElement) jsonEl).getAsJsonObject();
                                            softwareResponse.setJsonObj(jsonObj);
                                        }
                                    } catch (Exception e) {
                                        LOG.log(Level.WARNING, "Unable to parse response data: {0}", e.getMessage());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            String errorMessage = "Code Time: Unable to get the response from the http request, error: " + e.getMessage();
                            softwareResponse.setErrorMessage(errorMessage);
                            LOG.log(Level.WARNING, errorMessage);
                        }
                    }
                    
                }
            } catch (InterruptedException | ExecutionException e) {
                String errorMessage = "Code Time: Unable to get the response from the http request, error: " + e.getMessage();
                softwareResponse.setErrorMessage(errorMessage);
                LOG.log(Level.WARNING, errorMessage);
            }
        }

        return softwareResponse;
    }
    
    protected static class SlackHttpManager implements Callable<HttpResponse> {
    
        public static final Logger LOG = Logger.getLogger("Software");

        private static final String baseApi = "https://slack.com/api";
        private static final HttpClient httpClient = HttpClientBuilder.create().build();

        private String payload;
        private String api;
        private String access_token;
        private String httpMethodName;
        
        public SlackHttpManager(String api, String access_token, String httpMethodName, JsonObject payload) {
            this.api = api;
            this.payload = payload != null ? payload.toString() : null;
            this.access_token = access_token;
            this.httpMethodName = httpMethodName;
        }

        @Override
        public HttpResponse call() {
            HttpUriRequest req = null;
            try {
                HttpResponse response = null;

                switch (httpMethodName) {
                    case HttpPost.METHOD_NAME:
                    case HttpPut.METHOD_NAME:
                        req = new HttpPost(baseApi + this.api);
                        if (payload != null) {
                            // add the payload to the entity
                            StringEntity params = new StringEntity(payload);
                            ((HttpPost)req).setEntity(params);
                        }
                        break;
                    case HttpDelete.METHOD_NAME:
                        req = new HttpDelete(baseApi + "" + this.api);
                        break;
                    default:
                        req = new HttpGet(baseApi + "" + this.api);
                        break;
                }

                req.addHeader("Authorization", "Bearer " + access_token);

                req.addHeader("Content-type", "application/json; charset=utf-8");

                if (payload != null) {
                    LOG.log(Level.INFO, "Sofware.com: Sending API request: {0}, payload: {1}", new Object[]{api, payload});
                } else {
                    LOG.log(Level.INFO, "Sofware.com: Sending API request: {0}", api);
                }

                // execute the request
                response = httpClient.execute(req);

                //
                // Return the response
                //
                return response;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Code Time: Unable to make api request.{0}", e.getMessage());
            }

            return null;
        }
    }
}
