/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.swdc.netbeans.plugin.managers.SoftwareHttpManager;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.managers.EventTrackerManager;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.JsonManager;
import com.swdc.netbeans.plugin.managers.OfflineManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.managers.StatusBarManager;
import com.swdc.netbeans.plugin.models.FileDetails;
import com.swdc.netbeans.plugin.models.Integration;
import com.swdc.netbeans.plugin.models.NetbeansProject;
import com.swdc.netbeans.plugin.models.SoftwareUser;
import com.swdc.netbeans.plugin.models.UserLoginState;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.parsing.api.Source;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 *
 */
public class SoftwareUtil {

    public static final Logger LOG = Logger.getLogger("SoftwareUtil");

    public static final String CODENAME = "com.swdc.netbeans.plugin";

    private final static Object UTIL_LOCK = new Object();
    private static SoftwareUtil instance = null;
    
    public static final Gson gson = new GsonBuilder().create();

    // set the api endpoint to use
    public final static String API_ENDPOINT = "https://api.software.com";
    // set the launch url to use
    public final static String LAUNCH_URL = "https://app.software.com";
    
    private static int DASHBOARD_LABEL_WIDTH = 25;
    private static int DASHBOARD_VALUE_WIDTH = 25;
    private static int MARKER_WIDTH = 4;
    
    private static String SERVICE_NOT_AVAIL =
            "Our service is temporarily unavailable.\n\nPlease try again later.\n";
    
    private static int lastDayOfMonth = 0;
    
    // netbeans plugin id
    public final static int PLUGIN_ID = 11;

    public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

    public final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    public static HttpClient httpClient;
    public static HttpClient pingClient;
    
    private static final long DAYS_IN_SECONDS = 60 * 60 * 24;
    
    private final static int EOF = -1;
    
    private static String workspace_name = null;
    
    private final static Map<String, String> sessionMap = new HashMap<String, String>();
    
    public static boolean TELEMETRY_ON = true;
    
    private static boolean appAvailable = true;
    private static boolean loggedInCacheState = false;
    private static long lastAppAvailableCheck = 0;
    
    public static final String UNTITLED_FILE = "Untitled";
    public static final String UNNAMED_PROJECT = "Unnamed";
    
    private static boolean showStatusText = true;
    
    private static Document lastDocument = null;
    
    // cached result of OS detection
    protected static OSType detectedOS;
    
    static {
        // initialize the HttpClient
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();

        pingClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        httpClient = HttpClientBuilder.create().build();
    }
    
    private static String regex = "^\\S+@\\S+\\.\\S+$";
    private static Pattern pattern = Pattern.compile(regex);
    
    public enum OSType {
        Windows, MacOS, Linux, Other
    };
    
    public static OSType getOperatingSystemType() {
        if (detectedOS == null) {
          String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
          if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
            detectedOS = OSType.MacOS;
          } else if (OS.indexOf("win") >= 0) {
            detectedOS = OSType.Windows;
          } else if (OS.indexOf("nux") >= 0) {
            detectedOS = OSType.Linux;
          } else {
            detectedOS = OSType.Other;
          }
        }
        return detectedOS;
    }
    
    public static void updateServerStatus(boolean isOnlineStatus) {
        appAvailable = isOnlineStatus;
    }
    
    public static void updateLastDocument(Document doc) {
        lastDocument = doc;
    }
    
    public static boolean isAppAvailable() {
        return appAvailable;
    }
    
    public static class UserStatus {
        public boolean loggedIn;
    }
    
    public static String getHostname() {
        List<String> cmd = new ArrayList<String>();
        cmd.add("hostname");
        String hostname = getSingleLineResult(cmd, 1);
        return hostname;
    }
    
    public static String getWorkspaceName() {
        if (workspace_name == null) {
            workspace_name = generateToken();
        }
        return workspace_name;
    }

    public static String getVersion() {
        for (UpdateUnit updateUnit : UpdateManager.getDefault().getUpdateUnits()) {
            UpdateElement updateElement = updateUnit.getInstalled();
            if (updateElement != null) {
                if (SoftwareUtil.CODENAME.equals(updateElement.getCodeName())) {
                    return updateElement.getSpecificationVersion();
                }
            }
        }
        return "Unknown";
    }
    
    public static boolean softwareSessionFileExists() {
        // don't auto create the file
        String file = getSoftwareSessionFile(false);
        // check if it exists
        File f = new File(file);
        return f.exists();
    }
    
    public static boolean hasJwt() {
        String jwt = FileManager.getItem("jwt");
        return (jwt != null && !jwt.equals(""));
    }
    
    public static String getSummaryInfoFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (isWindows()) {
            file += "\\SummaryInfo.txt";
        } else {
            file += "/SummaryInfo.txt";
        }
        return file;
    };

    private static String getSoftwareSessionFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    private static String getSoftwareDataStoreFile() {
        String file = getSoftwareDir(true);
        if (isWindows()) {
            file += "\\data.json";
        } else {
            file += "/data.json";
        }
        return file;
    }
    
    public static String getCodeTimeDashboardFile() {
        String file = getSoftwareDir(true);
        if (isWindows()) {
            file += "\\CodeTime.txt";
        } else {
            file += "/CodeTime.txt";
        }
        return file;
    }
    
    public static String getReadmeFile() {
        String file = getSoftwareDir(true);
        if (isWindows()) {
            file += "\\netbeansCt_README.txt";
        } else {
            file += "/netbeansCt_README.txt";
        }
        return file;
    }

    public static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public static String getOs() {
        String osInfo = "";
        try {
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String osArch = System.getProperty("os.arch");

            if (osArch != null) {
                osInfo += osArch;
            }
            if (osInfo.length() > 0) {
                osInfo += "_";
            }
            if (osVersion != null) {
                osInfo += osVersion;
            }
            if (osInfo.length() > 0) {
                osInfo += "_";
            }
            if (osName != null) {
                osInfo += osName;
            }
        } catch (Exception e) {
                //
        }

        return osInfo;
    }

    public static boolean isWindows() {
        return getOperatingSystemType() == OSType.Windows;
    }

    public static boolean isMac() {
        return getOperatingSystemType() == OSType.MacOS;
    }
    
    public static boolean isLinux() {
        return (!isMac() && !isWindows());
    }

    public static String getSoftwareDir(boolean autoCreate) {
        String softwareDataDir = getUserHomeDir();
        if (isWindows()) {
            softwareDataDir += "\\.software";
        } else {
            softwareDataDir += "/.software";
        }

        File f = new File(softwareDataDir);
        if (autoCreate && !f.exists()) {
            // make the directory
            f.mkdirs();
        }

        return softwareDataDir;
    }

    public static String getStringRepresentation(HttpEntity res, boolean isPlainText) throws IOException {
        if (res == null) {
            return null;
        }

        InputStream inputStream = res.getContent();

        // Timing information--- verified that the data is still streaming
        // when we are called (this interval is about 2s for a large response.)
        // So in theory we should be able to do somewhat better by interleaving
        // parsing and reading, but experiments didn't show any improvement.
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader;
        reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));

        try (BufferedReader br = new BufferedReader(reader)) {
            boolean done = false;
            while (!done) {
                String aLine = br.readLine();
                if (aLine != null) {
                    sb.append(aLine);
                    if (isPlainText) {
                        sb.append("\n");
                    }
                } else {
                    done = true;
                }
            }
        }

        return sb.toString();
    }

    public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload) {
        return makeApiCall(api, httpMethodName, payload, null);
    }
    
    public static SoftwareResponse makeApiCall(String api, String httpMethodName, String payload, String overridingJwt) { 
        SoftwareResponse softwareResponse = new SoftwareResponse();
        if (!TELEMETRY_ON) {
            softwareResponse.setIsOk(true);
            return softwareResponse;
        }

        SoftwareHttpManager httpTask = null;
        if (api.contains("/ping") || api.contains("/sessions") || api.contains("/dashboard") || api.contains("/users/plugin/accounts")) {
            // if the server is having issues, we'll timeout within 5 seconds for these calls
            httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
        } else {
            if (httpMethodName.equals(HttpPost.METHOD_NAME)) {
                // continue, POSTS encapsulated in invoke laters with a timeout of 5 seconds
                httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
            } else {
                if (!appAvailable) {
                    // bail out
                    softwareResponse.setIsOk(false);
                    return softwareResponse;
                }
                httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, httpClient);
            }
        }
        Future<HttpResponse> response = EXECUTOR_SERVICE.submit(httpTask);

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
                            String jsonStr = getStringRepresentation(entity, isPlainText);
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

    private static void deleteFile(String file) {
        File f = new File(file);
        // if the file exists, delete it
        if (f.exists()) {
            f.delete();
        }
    }
    
    public static boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        boolean pastThreshold = (nowInSec - lastAppAvailableCheck > 120);
        if (pastThreshold) {
            SoftwareResponse resp = makeApiCall("/ping", HttpGet.METHOD_NAME, null);
            appAvailable = resp.isOk();
            lastAppAvailableCheck = nowInSec;
        }
        return appAvailable;
    }

    public static void showLoginPrompt() {
        
        boolean isOnline = isServerOnline();

        if (isOnline) {
            String msg = "To see your coding data in Code Time, please log in to your account.";

            Object[] options = {"Log in", "Not now"};
            int choice = JOptionPane.showOptionDialog(
                    null, msg, "Code Time", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                SoftwareSessionManager.launchLogin("email", UIInteractionType.keyboard, false);
            }
        }
    }
    
    public static void showAuthSelectPrompt(boolean isSignup) {
        String promptText = isSignup ? "Sign up" : "Log in";
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        String input = (String) JOptionPane.showInputDialog(
                null,
                promptText + " using",
                promptText,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options, // Array of choices
                options[0]); // Initial choice

        SoftwareSessionManager.launchLogin(input.toLowerCase(), UIInteractionType.click, true);
    }

    public static String generateToken() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }

    public static JsonObject getResourceInfo(String projectDir) {
        JsonObject jsonObj = new JsonObject();

        // is the project dir avail?
        if (projectDir != null && !projectDir.equals("")) {
            try {
                String[] branchCmd = {"git", "symbolic-ref", "--short", "HEAD"};
                String branch = runCommand(branchCmd, projectDir);

                String[] identifierCmd = {"git", "config", "--get", "remote.origin.url"};
                String identifier = runCommand(identifierCmd, projectDir);

                String[] emailCmd = {"git", "config", "user.email"};
                String email = runCommand(emailCmd, projectDir);

                String[] tagCmd = {"git", "describe", "--all"};
                String tag = runCommand(tagCmd, projectDir);

                if (branch != null && !branch.equals("") && identifier != null && !identifier.equals("")) {
                    jsonObj.addProperty("identifier", identifier);
                    jsonObj.addProperty("branch", branch);
                    jsonObj.addProperty("email", email);
                    jsonObj.addProperty("tag", tag);
                }
            } catch (Exception e) {
                //
            }
        }

        return jsonObj;
    }
    
    public static String runCommand(String[] args) {
        return runCommand(args, null);
    }
    
    public static String runCommand(String[] args, String dir) {
        String command = Arrays.toString(args);
        
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (StringUtils.isNotBlank(dir)) {
            processBuilder.directory(new File(dir));
        }

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            
            StringBuilder processOutput = new StringBuilder();
            BufferedReader processOutputReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String readLine;

            while ((readLine = processOutputReader.readLine()) != null) {
                processOutput.append(readLine).append(System.lineSeparator());
            }

            process.waitFor();
            String result = processOutput.toString().trim();
            return result;
        } catch (IOException | InterruptedException e) {
            LOG.log(Level.WARNING, "Code Time: Unable to complete command request: {0}", command);
        }

        return "";
    }
    
    public static String humanizeMinutes(int minutes) {
        String str = "";
        if (minutes == 60) {
            str = "1 hr";
        } else if (minutes > 60) {
            float hours = (float)minutes / 60;
            try {
                if (hours % 1 == 0) {
                    // don't return a number with 2 decimal place precision
                    str = String.format("%.0f", hours) + " hrs";
                } else {
                    hours = Math.round(hours * 10) / 10;
                    str = String.format("%.1f", hours) + " hrs";
                }
            } catch (Exception e) {
                str = String.valueOf(Math.round(hours)) + " hrs";
            }
        } else if (minutes == 1) {
            str = "1 min";
        } else {
            str = minutes + " min";
        }
        return str;
    }
    
    public static String humanizeDoubleNumbers(double number) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        return nf.format(number);
    }

    public static String humanizeLongNumbers(long number) {
        NumberFormat nf = NumberFormat.getInstance();
        return nf.format(number);
    }
    
    public static void launchSoftwareTopForty() {
        String url = "https://api.software.com/music/top40";
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }
    }
    
    public static void fetchCodeTimeMetricsDashboard(JsonObject summary) {
        OfflineManager offlineMgr = OfflineManager.getInstance();
        String summaryInfoFile = getSummaryInfoFile(true);
        String dashboardFile = getCodeTimeDashboardFile();
        
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        Writer writer = null;

        if (lastDayOfMonth == 0 || lastDayOfMonth != dayOfMonth) {
            lastDayOfMonth = dayOfMonth;
            String api = "/dashboard?linux=" + isLinux() + "&showToday=true";
            String dashboardSummary = makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
            if (dashboardSummary == null || dashboardSummary.trim().isEmpty()) {
                dashboardSummary = SERVICE_NOT_AVAIL;
            }

            // write the summary content
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(summaryInfoFile), StandardCharsets.UTF_8));
                writer.write(dashboardSummary);
            } catch (IOException ex) {
                // Report
            } finally {
                try {writer.close();} catch (Exception ex) {/*ignore*/}
            }
        }

        // concat summary info with the dashboard file
        String dashboardContent = "";

        // append the summary content
        String summaryInfoContent = offlineMgr.getSessionSummaryInfoFileContent();
        if (summaryInfoContent != null) {
            dashboardContent += summaryInfoContent;
        }

        // write the dashboard content to the dashboard file
        offlineMgr.saveFileContent(dashboardContent, dashboardFile);
    }
    
    public static void launchCodeTimeMetricsDashboard() {
        JsonObject sessionSummary = OfflineManager.getInstance().getSessionSummaryFileAsJson();
        fetchCodeTimeMetricsDashboard(sessionSummary);

        String codeTimeFile = getCodeTimeDashboardFile();
        File f = new File(codeTimeFile);
        
        try {
            // open the file in the editor
            FileObject fo = FileUtil.createData(f);
            DataObject d = DataObject.find(fo);
            NbDocument.openDocument(d, PLUGIN_ID, Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        // delete the legacy file
        String legacyFileName = codeTimeFile.substring(0, codeTimeFile.lastIndexOf("."));
        File legacyFile = new File(legacyFileName);
        if (legacyFile.exists()) {
            legacyFile.delete();
        }
    }
    
    private static String getSingleLineResult(List<String> cmd, int maxLen) {
        String result = null;
        String[] cmdArgs = Arrays.copyOf(cmd.toArray(), cmd.size(), String[].class);
        String content = runCommand(cmdArgs, null);

        // for now just get the 1st one found
        if (content != null) {
            String[] contentList = content.split("\n");
            if (contentList != null && contentList.length > 0) {
                int len = (maxLen != -1) ? Math.min(maxLen, contentList.length) : contentList.length;
                for (int i = 0; i < len; i++) {
                    String line = contentList[i];
                    if (line != null && line.trim().length() > 0) {
                        result = line.trim();
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String getOsUsername() {
        String username = System.getProperty("user.name");
        if (username == null || username.trim().equals("")) {
            try {
                List<String> cmd = new ArrayList<String>();
                if (isWindows()) {
                    cmd.add("cmd");
                    cmd.add("/c");
                    cmd.add("whoami");
                } else {
                    cmd.add("/bin/sh");
                    cmd.add("-c");
                    cmd.add("whoami");
                }
                username = getSingleLineResult(cmd, 1);
            } catch (Exception e) {
                //
            }
        }
        return username;
    }

    public static String createAnonymousUser(boolean ignoreJwt) {
        // make sure we've fetched the app jwt
        String jwt = FileManager.getItem("jwt");

        if (StringUtils.isBlank(jwt) || ignoreJwt) {
            String timezone = TimeZone.getDefault().getID();

            String plugin_uuid = FileManager.getPluginUuid();
            String auth_callback_state = FileManager.getAuthCallbackState();

            JsonObject payload = new JsonObject();
            payload.addProperty("username", getOsUsername());
            payload.addProperty("timezone", timezone);
            payload.addProperty("hostname", getHostname());
            payload.addProperty("auth_callback_state", auth_callback_state);
            payload.addProperty("plugin_uuid", plugin_uuid);

            String api = "/plugins/onboard";
            SoftwareResponse resp = SoftwareUtil.makeApiCall(api, HttpPost.METHOD_NAME, payload.toString());
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null && data.has("jwt")) {
                    String dataJwt = data.get("jwt").getAsString();
                    FileManager.setItem("jwt", dataJwt);
                    FileManager.setBooleanItem("switching_account", false);
                    FileManager.setAuthCallbackState(null);
                    return dataJwt;
                }
            }
        }
        return null;
    }
    
    public static UserLoginState getUserLoginState(boolean isIntegrationRequest) {
        UserLoginState userLoginState = new UserLoginState();
        SoftwareUser softwareUser = new SoftwareUser();
        
        String name = FileManager.getItem("name");

        String authType = FileManager.getItem("authType");
        if (StringUtils.isBlank(authType)) {
            authType = "software";
        }
        String jwt = FileManager.getItem("jwt");
        String auth_callback_state = FileManager.getAuthCallbackState();
        String token = (StringUtils.isNotBlank(auth_callback_state)) ? auth_callback_state : jwt;
        String api = "/users/plugin/state";
        SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null, token);

        boolean foundUser = (resp.isOk() && resp.getJsonObj() != null && resp.getJsonObj().has("user"));
        String state = (foundUser) ? resp.getJsonObj().get("state").getAsString() : "UNKNOWN";
        boolean isEmailLogin = (authType.equals("software") || authType.equals("email"));
        if (!state.equals("OK") && isEmailLogin) {
            // check using the jwt
            resp = makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
            foundUser = (resp.isOk() && resp.getJsonObj() != null && resp.getJsonObj().has("user"));
        }

        if (foundUser) {
            JsonObject obj = resp.getJsonObj().get("user").getAsJsonObject();
            // get the integrations
            JsonArray jsonIntegrations = JsonManager.getArrayVal(obj, "integrations");
            
            String payload_jwt = JsonManager.getStringVal(resp.getJsonObj(), "jwt", null);
            String plugin_jwt = JsonManager.getStringVal(obj, "plugin_jwt", null);

            softwareUser.plugin_jwt = plugin_jwt != null ? plugin_jwt : payload_jwt;
            softwareUser.registered = JsonManager.getIntVal(obj, "registered", 0);
            softwareUser.email = JsonManager.getStringVal(obj, "email", null);
            
            softwareUser.integrations = new ArrayList<>();
            if (jsonIntegrations.size() > 0) {
                for (JsonElement el : jsonIntegrations) {
                    Integration integration = new Integration();
                    integration.updateInfoWithResponse(el.getAsJsonObject());
                    softwareUser.integrations.add(integration);
                }
            }

            if (StringUtils.isNotBlank(plugin_jwt)) {
                FileManager.setItem("jwt", plugin_jwt);
            }
            if (softwareUser.registered == 1) {
                FileManager.setItem("name", softwareUser.email);
            }

            FileManager.setBooleanItem("switching_account", false);
            FileManager.setAuthCallbackState(null);
            
            userLoginState.user = softwareUser;
            userLoginState.loggedIn = (softwareUser.registered == 1);
        }

        return userLoginState;
    }
    
    public static String getDecodedUserIdFromJwt(String jwt) {
        String stippedDownJwt = jwt.indexOf("JWT ") != -1 ? jwt.substring("JWT ".length()) : jwt;
        try {
            String[] split_string = stippedDownJwt.split("\\.");
            String base64EncodedBody = split_string[1];

            org.apache.commons.codec.binary.Base64 base64Url = new Base64(true);
            String body = new String(base64Url.decode(base64EncodedBody));
            Map<String, String> jsonMap;

            ObjectMapper mapper = new ObjectMapper();
            // convert JSON string to Map
            jsonMap = mapper.readValue(body,
                    new TypeReference<Map<String, String>>() {
                    });
            Object idVal = jsonMap.getOrDefault("id", "");
            return idVal.toString();
        } catch (Exception ex) {}
        return "";
    }
    
    public static String getDashboardRow(String label, String value) {
        String content = getDashboardLabel(label) + " : " + getDashboardValue(value) + "\n";
        return content;
    }

    public static String getSectionHeader(String label) {
        String content = label + "\n";
        // add 3 to account for the " : " between the columns
        int dashLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH + 15;
        for (int i = 0; i < dashLen; i++) {
            content += "-";
        }
        content += "\n";
        return content;
    }

    public static String getDashboardLabel(String label) {
        return getDashboardDataDisplay(DASHBOARD_LABEL_WIDTH, label);
    }

    public static String getDashboardValue(String value) {
        String valueContent = getDashboardDataDisplay(DASHBOARD_VALUE_WIDTH, value);
        String paddedContent = "";
        for (int i = 0; i < 11; i++) {
            paddedContent += " ";
        }
        paddedContent += valueContent;
        return paddedContent;
    }

    public static String getDashboardDataDisplay(int widthLen, String data) {
        int len = widthLen - data.length();
        String content = "";
        for (int i = 0; i < len; i++) {
            content += " ";
        }
        return content + "" + data;
    }
    
    public static Project getProjectForPath(String fullFileName) {
        File f = new File(fullFileName);
        return FileOwnerQuery.getOwner(f.toURI());
    }
    
    public static Project getProject(Document d) {
        Source source = Source.create(d);
        if (source == null) {
            return null;
        }
        FileObject fileObject = source.getFileObject();
        if (fileObject == null) {
            return null;
        }
        return FileOwnerQuery.getOwner(fileObject);
    }
    
    public static Project getFirstActiveProject() {
        Set<Project> modifiedProjects = ProjectManager.getDefault().getModifiedProjects();
        if (modifiedProjects.size() > 0) {
            // return the 1st one
            return modifiedProjects.iterator().next();
        }
        
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc != null) {
            Document d = jtc.getDocument();
            return getProject(d);
        }
        return new NetbeansProject();
    }
    
    public static FileDetails getFileDetails(String fullFileName) {
        FileDetails fileDetails = new FileDetails();
        if (StringUtils.isNotBlank(fullFileName)) {
            fileDetails.full_file_name = fullFileName;
            
            File f = new File(fullFileName);

            if (f.exists()) {
                Project p = getProjectForPath(fullFileName);
                if (p != null) {
                    fileDetails.project_directory = p.getProjectDirectory().getPath();
                    fileDetails.project_name = p.getProjectDirectory().getName();
                }
            
                fileDetails.character_count = f.length();
                fileDetails.file_name = f.getName();
                if (StringUtils.isNotBlank(fileDetails.project_directory) && fullFileName.indexOf(fileDetails.project_directory) != -1) {
                    // strip out the project_file_name
                    String[] parts = fullFileName.split(fileDetails.project_directory);
                    if (parts.length > 1) {
                        fileDetails.project_file_name = parts[1];
                    } else {
                        fileDetails.project_file_name = fullFileName;
                    }
                } else {
                    fileDetails.project_file_name = fullFileName;
                }
                fileDetails.line_count = getLineCount(fullFileName);
                
                fileDetails.syntax = (fullFileName.contains(".")) ? fullFileName.substring(fullFileName.lastIndexOf(".") + 1) : "";
 
            }
        }

        return fileDetails;
    }

    public static int getLineCount(String fileName) {
        Stream<String> stream = null;
        try {
            Path path = Paths.get(fileName);
            stream = Files.lines(path);
            return (int) stream.count();
        } catch (Exception e) {
            return 0;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    //
                }
            }
        }
    }
    
    public static Date atStartOfWeek(long local_now) {
        // find out how many days to go back
        int daysBack = 0;
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            while (dayOfWeek != Calendar.SUNDAY) {
                daysBack++;
                dayOfWeek -= 1;
            }
        } else {
            daysBack = 7;
        }

        long startOfDayInSec = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
        long startOfWeekInSec = startOfDayInSec - (DAYS_IN_SECONDS * daysBack);

        return new Date(startOfWeekInSec * 1000);
    }
    
    public static Date atStartOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
        return localDateTimeToDate(startOfDay);
    }

    

    public static Date atEndOfDay(Date date) {
        LocalDateTime localDateTime = dateToLocalDateTime(date);
        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
        return localDateTimeToDate(endOfDay);
    }

    private static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // the timestamps are all in seconds
    public static class TimesData {
        public Integer offset;
        public long now;
        public long local_now;
        public String timezone;
        public long local_start_day;
        public long local_start_yesterday;
        public Date local_start_of_week_date;
        public Date local_start_of_yesterday_date;
        public Date local_start_today_date;
        public long local_start_of_week;
        public long local_end_day;
        public long utc_end_day;

        public TimesData() {
            offset = ZonedDateTime.now().getOffset().getTotalSeconds();
            now = System.currentTimeMillis() / 1000;
            local_now = now + offset;
            timezone = TimeZone.getDefault().getID();
            local_start_day = atStartOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            local_start_yesterday = local_start_day - DAYS_IN_SECONDS;
            local_start_of_week_date = atStartOfWeek(local_now);
            local_start_of_yesterday_date = new Date(local_start_yesterday * 1000);
            local_start_today_date = new Date(local_start_day * 1000);
            local_start_of_week = local_start_of_week_date.toInstant().getEpochSecond();
            local_end_day = atEndOfDay(new Date(local_now * 1000)).toInstant().getEpochSecond();
            utc_end_day = atEndOfDay(new Date(now * 1000)).toInstant().getEpochSecond();
        }
    }

    public static TimesData getTimesData() {
        TimesData timesData = new TimesData();
        return timesData;
    }
    
    public static Date getJavaDateFromSeconds(long seconds) {
        Instant instant = Instant.ofEpochSecond( seconds );
        Date date = Date.from( instant );
        return date;
    }
    
    public static String getTimeOfDay(Date d) {
        return new SimpleDateFormat("h:mm a").format(d);
    }

    public static String getTodayInStandardFormat() {
        SimpleDateFormat formatDay = new SimpleDateFormat("YYYY-MM-dd");
        String day = formatDay.format(new Date());
        return day;
    }

    public static boolean isNewDay() {
        String currentDay = FileManager.getItem("currentDay", "");
        String day = getTodayInStandardFormat();
        return !day.equals(currentDay);
    }
    
    public static boolean isGitProject(String projectDir) {
        if (projectDir == null || projectDir.equals("")) {
            return false;
        }

        String gitFile = projectDir + File.separator + ".git";
        File f = new File(gitFile);
        return f.exists();
    }

    public static JsonArray readAsJsonArray(String data) {
        try {
            JsonArray jsonArray = gson.fromJson(buildJsonReader(data), JsonArray.class);
            return jsonArray;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject readAsJsonObject(String data) {
        try {
            JsonObject jsonObject = gson.fromJson(buildJsonReader(data), JsonObject.class);
            return jsonObject;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonElement readAsJsonElement(String data) {
        try {
            JsonElement jsonElement = gson.fromJson(buildJsonReader(data), JsonElement.class);
            return jsonElement;
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonReader buildJsonReader(String data) {
        // Clean the data
        data = cleanJsonString(data);
        JsonReader reader = new JsonReader(new StringReader(data));
        reader.setLenient(true);
        return reader;
    }

    /**
     * Replace byte order mark, new lines, and trim
     * @param data
     * @return clean data
     */
    public static String cleanJsonString(String data) {
        data = data.replace("\ufeff", "").replace("/\r\n/g", "").replace("/\n/g", "").trim();

        int braceIdx = data.indexOf("{");
        int bracketIdx = data.indexOf("[");

        // multi editor writes to the data.json file can cause an undefined string before the json object, remove it
        if (braceIdx > 0 && (braceIdx < bracketIdx || bracketIdx == -1)) {
            // there's something before the 1st brace
            data = data.substring(braceIdx);
        } else if (bracketIdx > 0 && (bracketIdx < braceIdx || braceIdx == -1)) {
            // there's something before the 1st bracket
            data = data.substring(bracketIdx);
        }

        return data;
    }
    
    public static List<String> getResultsForCommandArgs(String[] args, String dir) {
        List<String> results = new ArrayList<>();
        try {
            String result = runCommand(args, dir);
            if (result == null || result.trim().length() == 0) {
                return results;
            }
            String[] contentList = result.split("\n");
            results = Arrays.asList(contentList);
        } catch (Exception e) {
            if (results == null) {
                results = new ArrayList<>();
            }
        }
        return results;
    }

    private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {

        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
    
    public static Project getOpenProject() {
        if (lastDocument != null) {
            return getProject(lastDocument);
        }
        return getFirstActiveProject();
    }
    
    public static void launchFile(String fsPath) {
        File f = new File(fsPath);
        
        try {
            // open the file in the editor
            FileObject fo = FileUtil.createData(f);
            DataObject d = DataObject.find(fo);
            NbDocument.openDocument(d, PLUGIN_ID, Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static boolean showingStatusText() {
        return showStatusText;
    }
    
    public static void toggleStatusBar(UIInteractionType interactionType) {
        String cta_text = !showStatusText ? "Show status bar metrics" : "Hide status bar metrics";
        showStatusText = !showStatusText;

        StatusBarManager.updateStatusBar();

        UIElementEntity elementEntity = new UIElementEntity();
        elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_toggle_status_bar_metrics_btn" : "ct_toggle_status_bar_metrics_cmd";
        elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
        elementEntity.color = interactionType == UIInteractionType.click ? "blue" : null;
        elementEntity.cta_text = cta_text;
        elementEntity.icon_name = interactionType == UIInteractionType.click ? "slash-eye" : null;
        EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);
    }
    
    public static String buildQueryString(JsonObject obj) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = obj.keySet().iterator();
        while(keys.hasNext()) {
            String key = keys.next();
            if (!obj.get(key).isJsonNull()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }

                String val = obj.get(key).getAsString();
                try {
                    val = URLEncoder.encode(val, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    LOG.log(Level.INFO, "Unable to url encode value, error: {0}", e.getMessage());
                }
                sb.append(key).append("=").append(val);
            }
        }
        return "?" + sb.toString();
    }

}
