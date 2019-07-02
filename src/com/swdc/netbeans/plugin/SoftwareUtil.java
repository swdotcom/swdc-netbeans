/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.javafx.PlatformUtil;
import com.swdc.netbeans.plugin.managers.SoftwareHttpManager;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.managers.OfflineManager;
import com.swdc.netbeans.plugin.managers.SessionManager;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar;
import com.swdc.netbeans.plugin.status.SoftwareStatusBar.StatusBarType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JOptionPane;
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

    // set the api endpoint to use
    // "http://localhost:5000", "https://qaapi.software.com", "https://stagingapi.software.com", "https://api.software.com"
    private final static String PROD_API_ENDPOINT = "https://api.software.com";
    // set the launch url to use
    // "http://localhost:3000", "https://qa.software.com", "https://staging.software.com", "https://app.software.com"
    private final static String PROD_URL_ENDPOINT = "https://app.software.com";

    // set the api endpoint to use
    public final static String API_ENDPOINT = PROD_API_ENDPOINT;
    // set the launch url to use
    public final static String LAUNCH_URL = PROD_URL_ENDPOINT;
    
    private static int DASHBOARD_LABEL_WIDTH = 25;
    private static int DASHBOARD_VALUE_WIDTH = 25;
    private static int MARKER_WIDTH = 4;
    
    private static String SERVICE_NOT_AVAIL =
            "Our service is temporarily unavailable.\n\nPlease try again later.\n";
    
    private static long lastDashboardFetchTime = 0;
    
    private final static long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    
    // netbeans plugin id
    public final static int PLUGIN_ID = 11;

    public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    public static HttpClient httpClient;
    public static HttpClient pingClient;

    public static JsonParser jsonParser = new JsonParser();
    public static Gson gson = new Gson();
    
    private final static Map<String, String> sessionMap = new HashMap<String, String>();
    
    public static boolean TELEMETRY_ON = true;
    
    private boolean appAvailable = true;
    private static boolean loggedInCacheState = false;
    private static long lastAppAvailableCheck = 0;
    
    private SoftwareStatusBar statusBar;
    
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

    public static SoftwareUtil getInstance() {
        synchronized (UTIL_LOCK) {
            if (instance == null) {
                instance = new SoftwareUtil();
                
            }
        }
        return instance;
    }
    
    private SoftwareUtil() {
        if (statusBar == null) {
            statusBar = new SoftwareStatusBar();
        }
    }
    
    private static String regex = "^\\S+@\\S+\\.\\S+$";
    private static Pattern pattern = Pattern.compile(regex);

    private static boolean validateEmail(String email) {
        return pattern.matcher(email).matches();
    }
    
    public class UserStatus {
        public boolean loggedIn;
    }
    
    public void toggleStatusBar() {
        statusBar.toggleStatusBarText();
    }

    
    public void updateServerStatus(boolean isOnlineStatus) {
        appAvailable = isOnlineStatus;
    }
    
    public String getHostname() {
        List<String> cmd = new ArrayList<String>();
        cmd.add("hostname");
        String hostname = getSingleLineResult(cmd, 1);
        return hostname;
    }

    public String getVersion() {
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

    public String getItem(String key) {
        String val = sessionMap.get(key);
        if (val != null) {
            return val;
        }
        JsonObject jsonObj = getSoftwareSessionAsJson();
        if (jsonObj != null && jsonObj.has(key) && !jsonObj.get(key).isJsonNull()) {
            return jsonObj.get(key).getAsString();
        }
        return null;
    }

    public void setItem(String key, String val) {
        sessionMap.put(key, val);
        
        JsonObject jsonObj = getSoftwareSessionAsJson();
        jsonObj.addProperty(key, val);

        String content = jsonObj.toString();

        String sessionFile = getSoftwareSessionFile(true);

        try {
            try (Writer output = new BufferedWriter(new FileWriter(sessionFile))) {
                output.write(content);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Code Time: Failed to write the key value pair ({0}, {1}) into the session, error: {2}", new Object[]{key, val, e.getMessage()});
        }
    }
    
    public boolean softwareSessionFileExists() {
        // don't auto create the file
        String file = getSoftwareSessionFile(false);
        // check if it exists
        File f = new File(file);
        return f.exists();
    }
    
    public boolean hasJwt() {
        String jwt = getItem("jwt");
        return (jwt != null && !jwt.equals(""));
    }

    private JsonObject getSoftwareSessionAsJson() {
        JsonObject data = null;

        String sessionFile = getSoftwareSessionFile(true);
        File f = new File(sessionFile);
        if (f.exists()) {
            try {
                byte[] encoded = Files.readAllBytes(Paths.get(sessionFile));
                String content = new String(encoded, Charset.defaultCharset());
                if (content != null) {
                    // json parse it
                    data = jsonParser.parse(content).getAsJsonObject();
                }
            } catch (JsonSyntaxException | IOException e) {
                LOG.log(Level.WARNING, "Code Time: Error trying to read and json parse the session file.{0}", e.getMessage());
            }
        }
        return (data == null) ? new JsonObject() : data;
    }
    
    public String getSummaryInfoFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (this.isWindows()) {
            file += "\\SummaryInfo.txt";
        } else {
            file += "/SummaryInfo.txt";
        }
        return file;
    };

    private String getSoftwareSessionFile(boolean autoCreate) {
        String file = getSoftwareDir(autoCreate);
        if (isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    private String getSoftwareDataStoreFile() {
        String file = getSoftwareDir(true);
        if (isWindows()) {
            file += "\\data.json";
        } else {
            file += "/data.json";
        }
        return file;
    }
    
    public String getCodeTimeDashboardFile() {
        String file = getSoftwareDir(true);
        if (isWindows()) {
            file += "\\CodeTime.txt";
        } else {
            file += "/CodeTime.txt";
        }
        return file;
    }

    public void storePayload(String payload) {
        if (payload == null || payload.length() == 0) {
            return;
        }
        if (isWindows()) {
            payload += "\r\n";
        } else {
            payload += "\n";
        }
        String dataStoreFile = getSoftwareDataStoreFile();
        File f = new File(dataStoreFile);
        try {
            Writer output;
            output = new BufferedWriter(new FileWriter(f, true));  //clears file every time
            output.append(payload);
            output.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Code Time: Error appending to the Software data store file, error: {0}", e.getMessage());
        }
    }

    public String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    public String getOs() {
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

    public boolean isWindows() {
        return (PlatformUtil.isWindows());
    }

    public boolean isMac() {
        return (PlatformUtil.isMac());
    }
    
    public boolean isLinux() {
        return (!isMac() && !isWindows());
    }

    public String getSoftwareDir(boolean autoCreate) {
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

    public Date atStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private String getStringRepresentation(HttpEntity res, boolean isPlainText) throws IOException {
        if (res == null) {
            return null;
        }

        InputStream inputStream = res.getContent();

        // Timing information--- verified that the data is still streaming
        // when we are called (this interval is about 2s for a large response.)
        // So in theory we should be able to do somewhat better by interleaving
        // parsing and reading, but experiments didn't show any improvement.
        //
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

    public SoftwareResponse makeApiCall(String api, String httpMethodName, String payload) {
        return this.makeApiCall(api, httpMethodName, payload, null);
    }
    
    public SoftwareResponse makeApiCall(String api, String httpMethodName, String payload, String overridingJwt) { 
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
                                Object jsonEl = jsonParser.parse(jsonStr);
                                
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
                    
                    if (statusCode >= 400 && statusCode < 500 && jsonObj != null) {
                        if (jsonObj.has("code")) {
                            String code = jsonObj.get("code").getAsString();
                            if (code != null && code.equals("DEACTIVATED")) {
                                SoftwareUtil.getInstance().setStatusLineMessage(
                                    StatusBarType.ALERT,
                                    "Code Time",
                                    "To see your coding data in Code Time, please reactivate your account.");
                                softwareResponse.setDeactivated(true);
                            }
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

    public void sendOfflineData() {
        String dataStoreFile = getSoftwareDataStoreFile();
        File f = new File(dataStoreFile);

        if (f.exists()) {
            // JsonArray jsonArray = new JsonArray();
            StringBuilder sb = new StringBuilder();
            try {
                FileInputStream fis = new FileInputStream(f);

                try ( //Construct BufferedReader from InputStreamReader
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        if (line.length() > 0) {
                            sb.append(line).append(",");
                        }
                    }
                }

                if (sb.length() > 0) {
                    String payloads = sb.toString();
                    payloads = payloads.substring(0, payloads.lastIndexOf(","));
                    payloads = "[" + payloads + "]";
                    SoftwareResponse resp = makeApiCall("/data/batch", HttpPost.METHOD_NAME, payloads);
                    if (resp.isOk() || resp.isDeactivated()) {
                        deleteFile(dataStoreFile);
                    }
                } else {
                    LOG.log(Level.INFO, "Code Time: No offline data to send");
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Code Time: Error trying to read and send offline data, error: {0}", e.getMessage());
            }
        }
    }

    private void deleteFile(String file) {
        File f = new File(file);
        // if the file exists, delete it
        if (f.exists()) {
            f.delete();
        }
    }
    
    public boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        boolean pastThreshold = (nowInSec - lastAppAvailableCheck > 60);
        if (pastThreshold) {
            SoftwareResponse resp = this.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
            this.updateServerStatus(resp.isOk());
            lastAppAvailableCheck = nowInSec;
        }
        return appAvailable;
    }

    public void showLoginPrompt() {
        
        boolean isOnline = isServerOnline();

        if (isOnline) {
            String msg = "To see your coding data in Code Time, please log in to your account.";

            Object[] options = {"Log in", "Not now"};
            int choice = JOptionPane.showOptionDialog(
                    null, msg, "Code Time", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                SoftwareUtil.getInstance().launchLogin();
            }
        }
    }

    public String generateToken() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }

    public JsonObject getResourceInfo(String projectDir) {
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
    
    public String runCommand(String[] args, String dir) {
        String command = Arrays.toString(args);
        
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (dir != null) {
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
    
    public void updateTelementry(boolean telemetryOn) {
        TELEMETRY_ON = telemetryOn;
        if (!TELEMETRY_ON) {
            setStatusLineMessage(StatusBarType.ALERT, "<S> Paused", "Enable metrics to resume");
        } else {
            setStatusLineMessage(StatusBarType.NO_KPM, "Code Time", "Click to log in to Code Time");
        }
    }
    
    public String humanizeMinutes(int minutes) {
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
    
    public void launchSoftwareTopForty() {
        String url = "https://api.software.com/music/top40";
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }
    }
    
    public void fetchCodeTimeMetricsDashboard(JsonObject summary) {
        OfflineManager offlineMgr = OfflineManager.getInstance();
        String summaryInfoFile = this.getSummaryInfoFile(true);
        String dashboardFile = this.getCodeTimeDashboardFile();
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        long diff = nowInSec - lastDashboardFetchTime;
        Writer writer = null;

        File f = new File(summaryInfoFile);
        if (!f.exists() || lastDashboardFetchTime == 0 || diff >= MILLIS_PER_DAY) {
            lastDashboardFetchTime = nowInSec;
            String api = "/dashboard?linux=" + this.isLinux() + "&showToday=false";
            String dashboardSummary = this.makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
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
        SimpleDateFormat formatDayTime = new SimpleDateFormat("EEE, MMM d h:mma");
        SimpleDateFormat formatDay = new SimpleDateFormat("EEE, MMM d");
        String lastUpdatedStr = formatDayTime.format(new Date());
        dashboardContent += "Code Time          (Last updated on " + lastUpdatedStr + ")";
        dashboardContent += "\n\n";
        String todayStr = formatDay.format(new Date());
        dashboardContent += getSectionHeader("Today (" + todayStr + ")");


        if (summary != null) {
            int currentDayMinutes = 0;
            if (summary.has("currentDayMinutes")) {
                currentDayMinutes = summary.get("currentDayMinutes").getAsInt();
            }
            int averageDailyMinutes = 0;
            if (summary.has("averageDailyMinutes")) {
                averageDailyMinutes = summary.get("averageDailyMinutes").getAsInt();
            }

            String currentDayTimeStr = this.humanizeMinutes(currentDayMinutes);
            String averageDailyMinutesTimeStr = this.humanizeMinutes(averageDailyMinutes);

            dashboardContent += getDashboardRow("Hours coded today", currentDayTimeStr);
            dashboardContent += getDashboardRow("90-day avg", averageDailyMinutesTimeStr);
            dashboardContent += "\n";
        }

        // append the summary content
        String summaryInfoContent = offlineMgr.getSessionSummaryInfoFileContent();
        if (summaryInfoContent != null) {
            dashboardContent += summaryInfoContent;
        }

        // write the dashboard content to the dashboard file
        offlineMgr.saveFileContent(dashboardContent, dashboardFile);
    }
    
    public void launchCodeTimeMetricsDashboard() {
        SessionManager.fetchDailyKpmSessionInfo(false);
        JsonObject sessionSummary = OfflineManager.getInstance().getSessionSummaryFileAsJson();
        fetchCodeTimeMetricsDashboard(sessionSummary);

        String codeTimeFile = this.getCodeTimeDashboardFile();
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

    public void setStatusLineMessage(final StatusBarType barType, final String statusMsg, final String tooltip) {
        statusBar.updateMessage(barType, statusMsg, tooltip);
    }
    
    private String getSingleLineResult(List<String> cmd, int maxLen) {
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

    public String getOsUsername() {
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

    public String getAppJwt(boolean serverIsOnline) {
        // clear out the previous app_jwt
        setItem("app_jwt", null);

        if (serverIsOnline) {
            long now = Math.round(System.currentTimeMillis() / 1000);
            String api = "/data/apptoken?token=" + now;
            SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null);
            if (resp.isOk()) {
                JsonObject obj = resp.getJsonObj();
                return obj.get("jwt").getAsString();
            }
        }
        return null;
    }

    public String createAnonymousUser(boolean serverIsOnline) {
        // make sure we've fetched the app jwt
        String appJwt = getAppJwt(serverIsOnline);

        if (serverIsOnline && appJwt != null) {
            String timezone = TimeZone.getDefault().getID();

            JsonObject payload = new JsonObject();
            payload.addProperty("username", getOsUsername());
            payload.addProperty("timezone", timezone);
            payload.addProperty("hostname", getHostname());
            payload.addProperty("creation_annotation", "NO_SESSION_FILE");

            String api = "/data/onboard";
            SoftwareResponse resp = makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), appJwt);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null && data.has("jwt")) {
                    String dataJwt = data.get("jwt").getAsString();
                    setItem("jwt", dataJwt);
                    return dataJwt;
                }
            }
        }
        return null;
    }
    
    private JsonObject getUser(boolean serverIsOnline) {
        String jwt = getItem("jwt");
        if (serverIsOnline) {
            String api = "/users/me";
            SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject obj = resp.getJsonObj();
                if (obj != null && obj.has("data")) {
                    return obj.get("data").getAsJsonObject();
                }
            }
        }
        return null;
    }

    private boolean isLoggedOn(boolean serverIsOnline) {
        String jwt = getItem("jwt");
        if (serverIsOnline) {
            JsonObject userObj = getUser(serverIsOnline);
            if (userObj != null && userObj.has("email")) {
                // check if the email is valid
                String email = userObj.get("email").getAsString();
                if (validateEmail(email)) {
                    setItem("jwt", userObj.get("plugin_jwt").getAsString());
                    setItem("name", email);
                    return true;
                }
            }
            String api = "/users/plugin/state";
            SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null, jwt);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                String state = (data != null && data.has("state")) ? data.get("state").getAsString() : "UNKNOWN";
                if (state.equals("OK")) {
                    String dataJwt = data.get("jwt").getAsString();
                    setItem("jwt", dataJwt);
                    String dataEmail = data.get("email").getAsString();
                    if (dataEmail != null) {
                        setItem("name", dataEmail);
                    }
                    return true;
                } else if (state.equals("NOT_FOUND")) {
                    setItem("jwt", null);
                }
            }
        }
        setItem("name", null);
        return false;
    }

    public UserStatus getUserStatus() {
        UserStatus currentUserStatus = new UserStatus();
        if (loggedInCacheState) {
            currentUserStatus.loggedIn = true;
            return currentUserStatus;
        }
        
        boolean serverIsOnline = isServerOnline();

        boolean loggedIn = isLoggedOn(serverIsOnline);

        
        currentUserStatus.loggedIn = loggedIn;
        
        if (loggedInCacheState != loggedIn) {
            // logged in state changed
            sendHeartbeat("STATE_CHANGE:LOGGED_IN:" + loggedIn);
            // refetch kpm
            new Thread(() -> {
                try {
                    SessionManager.fetchDailyKpmSessionInfo(true);
                } catch (Exception e) {
                    System.err.println(e);
                }
            }).start();
        }

        loggedInCacheState = loggedIn;

        return currentUserStatus;
    }
    
    public void sendHeartbeat(String reason) {
        boolean serverIsOnline = isServerOnline();
        String jwt = getItem("jwt");
        if (serverIsOnline && jwt != null) {

            long start = Math.round(System.currentTimeMillis() / 1000);

            JsonObject payload = new JsonObject();
            payload.addProperty("pluginId", SoftwareUtil.PLUGIN_ID);
            payload.addProperty("os", getOs());
            payload.addProperty("start", start);
            payload.addProperty("version", getVersion());
            payload.addProperty("hostname", getHostname());
            payload.addProperty("trigger_annotation", reason);

            String api = "/data/heartbeat";
            SoftwareResponse resp = makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), jwt);
            if (!resp.isOk()) {
                LOG.log(Level.WARNING, "Code Time: unable to send heartbeat ping");
            }
        }
    }
    
    protected void lazilyFetchUserStatus(int retryCount) {
        UserStatus userStatus = this.getUserStatus();

        if (!userStatus.loggedIn && retryCount > 0) {
            final int newRetryCount = retryCount - 1;
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                    lazilyFetchUserStatus(newRetryCount);
                }
                catch (InterruptedException e){
                    System.err.println(e);
                }
            }).start();
        }
    }

    public void launchLogin() {
        String url = LAUNCH_URL;
        String jwt = getItem("jwt");
        try {
            jwt = URLEncoder.encode(jwt, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            //
        }
        url += "/onboarding?token=" + jwt;

        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        new Thread(() -> {
            try {
                Thread.sleep(10000);
                lazilyFetchUserStatus(10);
            }
            catch (InterruptedException e){
                System.err.println(e);
            }
        }).start();
    }

    public void launchWebDashboard() {
        String url = LAUNCH_URL + "/login";
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }
    }
    
    public String getDashboardRow(String label, String value) {
        String content = getDashboardLabel(label) + " : " + getDashboardValue(value) + "\n";
        return content;
    }

    public String getSectionHeader(String label) {
        String content = label + "\n";
        // add 3 to account for the " : " between the columns
        int dashLen = DASHBOARD_LABEL_WIDTH + DASHBOARD_VALUE_WIDTH + 15;
        for (int i = 0; i < dashLen; i++) {
            content += "-";
        }
        content += "\n";
        return content;
    }

    public String getDashboardLabel(String label) {
        return getDashboardDataDisplay(DASHBOARD_LABEL_WIDTH, label);
    }

    public String getDashboardValue(String value) {
        String valueContent = getDashboardDataDisplay(DASHBOARD_VALUE_WIDTH, value);
        String paddedContent = "";
        for (int i = 0; i < 11; i++) {
            paddedContent += " ";
        }
        paddedContent += valueContent;
        return paddedContent;
    }

    public String getDashboardDataDisplay(int widthLen, String data) {
        int len = widthLen - data.length();
        String content = "";
        for (int i = 0; i < len; i++) {
            content += " ";
        }
        return content + "" + data;
    }
}
