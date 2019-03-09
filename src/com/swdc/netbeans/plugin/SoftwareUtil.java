/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.javafx.PlatformUtil;
import com.swdc.netbeans.plugin.managers.SoftwareHttpManager;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    private final static String PROD_API_ENDPOINT = "https://api.software.com";
    private final static String PROD_URL_ENDPOINT = "https://app.software.com";

    private final static long MILLIS_PER_HOUR = 1000 * 60 * 60;
    private final static int LONG_THRESHOLD_HOURS = 24;

    // set the api endpoint to use
    public final static String API_ENDPOINT = PROD_API_ENDPOINT;
    // set the launch url to use
    public final static String LAUNCH_URL = PROD_URL_ENDPOINT;
    // netbeans plugin id
    public final static int PLUGIN_ID = 11;

    public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    public static HttpClient httpClient;
    public static HttpClient pingClient;

    public static JsonParser jsonParser = new JsonParser();
    public static Gson gson = new Gson();
    
    private static Pattern patternMacPairs = Pattern.compile("([a-fA-F0-9]{2}[:\\.-]?){5}[a-fA-F0-9]{2}");
    private static Pattern patternMacTriples = Pattern.compile("([a-fA-F0-9]{3}[:\\.-]?){3}[a-fA-F0-9]{3}");
    private static Pattern patternMac = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
    
    public static boolean TELEMETRY_ON = true;
    
    private Long lastRegisterUserCheck = null;
    private UserStatus currentUserStatus = null;
    private boolean appAvailable = true;
    
    private SoftwareStatusBar statusBar;
    
    static {
        // initialize the HttpClient
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(3000)
                .setSocketTimeout(3000)
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
    
    public class UserStatus {
        public User loggedInUser;
        public String email;
        public boolean hasUserAccounts;
    }

    // "id", "email", "plugin_jwt", "mac_addr", "mac_addr_share"
    public class User {
        public Long id;
        public String email;
        public String plugin_jwt;
        public String mac_addr;
        public String mac_addr_share;
    }
    
    public void updateServerStatus(boolean isOnlineStatus) {
        appAvailable = isOnlineStatus;
    }

    public void clearUserStatusCache() {
        lastRegisterUserCheck = null;
        currentUserStatus = null;
    }

    public String getPluginVersion() {
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
        JsonObject jsonObj = getSoftwareSessionAsJson();
        if (jsonObj != null && !jsonObj.get(key).isJsonNull() && jsonObj.has(key)) {
            return jsonObj.get(key).getAsString();
        }
        return null;
    }

    public void setItem(String key, String val) {
        JsonObject jsonObj = getSoftwareSessionAsJson();
        jsonObj.addProperty(key, val);

        String content = jsonObj.toString();

        String sessionFile = getSoftwareSessionFile();

        try {
            try (Writer output = new BufferedWriter(new FileWriter(sessionFile))) {
                output.write(content);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Code Time: Failed to write the key value pair ({0}, {1}) into the session, error: {2}", new Object[]{key, val, e.getMessage()});
        }
    }

    private JsonObject getSoftwareSessionAsJson() {
        JsonObject data = null;

        String sessionFile = getSoftwareSessionFile();
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

    private String getSoftwareSessionFile() {
        String file = getSoftwareDir();
        if (isWindows()) {
            file += "\\session.json";
        } else {
            file += "/session.json";
        }
        return file;
    }

    private String getSoftwareDataStoreFile() {
        String file = getSoftwareDir();
        if (isWindows()) {
            file += "\\data.json";
        } else {
            file += "/data.json";
        }
        return file;
    }
    
    private String getCodeTimeDashboardFile() {
        String file = getSoftwareDir();
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

    public String getOsUserName() {
        return System.getProperty("user.name");
    }
    
    public String getOsInfo() {
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

    private String getSoftwareDir() {
        String softwareDataDir = getUserHomeDir();
        if (isWindows()) {
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
        reader = new InputStreamReader(inputStream);

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
            // if the server is having issues, we'll timeout within 3 seconds for these calls
            httpTask = new SoftwareHttpManager(api, httpMethodName, payload, overridingJwt, pingClient);
        } else {
            if (httpMethodName.equals(HttpPost.METHOD_NAME)) {
                // continue, POSTS encapsulated in invoke laters with a timeout of 3 seconds
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
        SoftwareResponse resp = this.makeApiCall("/ping", HttpGet.METHOD_NAME, null);
        boolean isOk = resp.isOk();
        this.updateServerStatus(isOk);
        return isOk;
    }

    public void checkUserAuthenticationStatus() {
        
        boolean isOnline = isServerOnline();
        UserStatus userStatus = this.getUserStatus();
        String lastUpdateTimeStr = getItem("jetbrains_lastUpdateTime");

        if (isOnline && lastUpdateTimeStr == null && !userStatus.hasUserAccounts) {
            setItem("netbeans_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
            String msg = "To see your coding data in Code Time, please log in to your account.";

            Object[] options = {"Log in", "Sign up", "Not now"};
            int choice = JOptionPane.showOptionDialog(
                    null, msg, "Code Time", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                SoftwareUtil.getInstance().launchLogin();
            } else if (choice == 1) {
                SoftwareUtil.getInstance().launchSignup();
            }
        }
    }

    public boolean isAuthenticated() {
        String tokenVal = getItem("token");
        if (tokenVal == null) {
            return false;
        }

        SoftwareResponse resp = makeApiCall("/users/ping/", HttpGet.METHOD_NAME, null);
        if (!resp.isOk() && !resp.isDeactivated()) {
            // update the status bar with Sign Up message
            setStatusLineMessage(StatusBarType.ALERT, "Code Time", "Click to log in to Code Time");
        }
        return resp.isOk();
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
    
    public boolean requiresAuthentication() {
        boolean requiresAuthentication = false;
        // create the token value
        String token = getItem("token");
        String jwt = getItem("jwt");
        boolean addToken = false;
        if (token == null || token.equals("")) {
            requiresAuthentication = true;
        } else if (jwt == null || jwt.equals("") || !isAuthenticated()) {
            requiresAuthentication = true;
        }
        return requiresAuthentication;
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
    
    public void fetchCodeTimeMetrics() {
        String api = "/dashboard";
        String dashboardContent = this.makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
        String codeTimeFile = this.getCodeTimeDashboardFile();

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(codeTimeFile), "utf-8"));
            writer.write(dashboardContent);
        } catch (IOException ex) {
            // Report
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {/*ignore*/}
        }
    }
    
    public void launchCodeTimeMetricsDashboard() {
        fetchCodeTimeMetrics();
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
    
    public FileTime getCreationTime(File file) throws IOException {
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        FileTime fileTime = attr.creationTime();
        return fileTime;
    }
    
    public static boolean isMacEmail(String email) {
        if (email.contains("_")) {
            String[] parts = email.split("_");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (patternMacPairs.matcher(part).find()
                        || patternMacTriples.matcher(part).find()
                        || patternMac.matcher(part).find()) {
                    return true;
                }
            }
        } else if (patternMacPairs.matcher(email).find()
                || patternMacTriples.matcher(email).find()
                || patternMac.matcher(email).find()) {
            return true;
        }
        return false;
    }

    public String getIdentity() {
        String identityId = null;

        try {
            List<String> cmd = new ArrayList<String>();
            if (isWindows()) {
                cmd.add("cmd");
                cmd.add("/c");
                cmd.add("wmic nic get MACAddress");
            } else {
                cmd.add("/bin/sh");
                cmd.add("-c");
                cmd.add("ifconfig | grep \"ether \" | grep -v 127.0.0.1 | cut -d \" \" -f2");
            }

            String[] cmdArgs = Arrays.copyOf(cmd.toArray(), cmd.size(), String[].class);
            String content = runCommand(cmdArgs, null);

            // for now just get the 1st one found
            if (content != null) {
                String[] contentList = content.split("\n");
                if (contentList != null && contentList.length > 0) {
                    for (String line : contentList) {
                        if (line != null && line.trim().length() > 0 &&
                                ( patternMacPairs.matcher(line).find()
                                        || patternMacTriples.matcher(line).find()
                                        || patternMac.matcher(line).find() ) ) {
                            identityId = line.trim();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //
        }
        String username = this.getOsUserName();
        String macAddrId = "";
        if (username != null) {
            macAddrId += username;
        }
        if (macAddrId.length() > 0) {
            macAddrId += "_";
        }
        if (identityId != null) {
            macAddrId += identityId;
        }
        return macAddrId;
    }

    public String getJsonObjString(JsonObject obj, String key) {
        if (obj != null && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    public Long getJsonObjLong(JsonObject obj, String key) {
        if (obj != null && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsLong();
        }
        return null;
    }

    public String getAppJwt(String macAddr) {
        setItem("app_jwt", null);
        boolean serverIsOnline = isServerOnline();
        if (serverIsOnline && macAddr != null) {
            String encodedMacIdentity = "";
            try {
                encodedMacIdentity = URLEncoder.encode(macAddr, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // url encoding failed, just use the mac addr id
                encodedMacIdentity = macAddr;
            }

            String api = "/data/token?addr=" + encodedMacIdentity;
            SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null);
            if (resp.isOk()) {
                JsonObject obj = resp.getJsonObj();
                return obj.get("jwt").getAsString();
            }
        }
        return null;
    }

    public void createAnonymousUser(String macAddr) {
        boolean serverIsOnline = isServerOnline();
        String pluginToken = getItem("token");
        // make sure we've fetched the app jwt
        String appJwt = getAppJwt(macAddr);

        if (serverIsOnline && macAddr != null) {
            String email = macAddr;
            if (pluginToken == null) {
                pluginToken = generateToken();
                setItem("token", pluginToken);
            }
            String timezone = TimeZone.getDefault().getID();

            String encodedMacIdentity = "";
            try {
                encodedMacIdentity = URLEncoder.encode(macAddr, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // url encoding failed, just use the mac addr id
                encodedMacIdentity = macAddr;
            }
            JsonObject payload = new JsonObject();
            payload.addProperty("email", email);
            payload.addProperty("plugin_token", pluginToken);
            payload.addProperty("timezone", timezone);
            String api = "/data/onboard?addr=" + encodedMacIdentity;
            SoftwareResponse resp = makeApiCall(api, HttpPost.METHOD_NAME, payload.toString(), appJwt);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt, user, and jetbrains_lastUpdateTime
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null && data.has("jwt")) {
                    String dataJwt = data.get("jwt").getAsString();
                    String user = data.get("user").getAsString();
                    setItem("jwt", dataJwt);
                    setItem("user", user);
                }
            }
        }
    }

    public List<User> getAuthenticatedPluginAccounts(String macAddr) {
        List<User> users = new ArrayList<>();
        boolean serverIsOnline = isServerOnline();

        // mac addr query str
        String api = "/users/plugin/accounts?token=";
        try {
            String encodedMacIdentity = URLEncoder.encode(macAddr, "UTF-8");
            api += encodedMacIdentity;
        } catch (UnsupportedEncodingException e) {
            // url encoding failed, just use the mac addr id
            api += macAddr;
        }

        if (serverIsOnline) {
            SoftwareResponse resp = makeApiCall(api, HttpGet.METHOD_NAME, null);
            if (resp.isOk()) {
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null && data.has("users")) {
                    try {
                        JsonArray jsonUsers = data.getAsJsonArray("users");
                        if (jsonUsers != null && jsonUsers.size() > 0) {
                            for (JsonElement userObj : jsonUsers) {
                                JsonObject obj = (JsonObject)userObj;
                                User user = new User();
                                user.email = getJsonObjString(obj, "email");
                                user.mac_addr = getJsonObjString(obj, "mac_addr");
                                user.mac_addr_share = getJsonObjString(obj, "mac_addr_share");
                                user.plugin_jwt = getJsonObjString(obj, "plugin_jwt");
                                user.id = getJsonObjLong(obj, "id");
                                users.add(user);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("error: " + e.getMessage());
                    }
                }
            }
        }

        return users;
    }

    public static User getLoggedInUser(String macAddr, List<User> authAccounts) {
        if (authAccounts != null && authAccounts.size() > 0) {
            for (User user : authAccounts) {
                String userMacAddr = (user.mac_addr != null) ? user.mac_addr : "";
                String userEmail = (user.email != null) ? user.email : "";
                String userMacAddrShare = (user.mac_addr_share != null) ? user.mac_addr_share : "";
                if (!userEmail.equals(userMacAddr) &&
                    !userEmail.equals(macAddr) &&
                    !userEmail.equals(userMacAddrShare) &&
                    userMacAddr.equals(macAddr)) {
                    return user;
                }
            }
        }
        return null;
    }
    
    public static boolean hasRegisteredUserAccount(List<User> authAccounts) {
        if (authAccounts != null && authAccounts.size() > 0) {
            for (User user : authAccounts) {
                if (user.email != null && !isMacEmail(user.email)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static User getAnonymousUser(List<User> authAccounts) {
        if (authAccounts != null && authAccounts.size() > 0) {
            for (User user : authAccounts) {
                if (user.email != null && isMacEmail(user.email)) {
                    return user;
                }
            }
        }
        return null;
    }

    private void updateSessionUser(User user) {
        JsonObject userObj = new JsonObject();
        userObj.addProperty("id", user.id);
        setItem("jwt", user.plugin_jwt);
        setItem("user", userObj.toString());
        setItem("jetbrains_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
    }

    public UserStatus getUserStatus() {
        long nowMillis = System.currentTimeMillis();
        if (currentUserStatus != null && lastRegisterUserCheck != null) {
            if (nowMillis - lastRegisterUserCheck.longValue() <= 5000) {
                return currentUserStatus;
            }
        }

        String identityId = getIdentity();

        if (currentUserStatus == null) {
            currentUserStatus = new UserStatus();
        }

        try {
            List<User> authAccounts = getAuthenticatedPluginAccounts(identityId);
            User loggedInUser = getLoggedInUser(identityId, authAccounts);
            User anonUser = getAnonymousUser(authAccounts);
            if (anonUser == null) {
                // create the anonymous user
                createAnonymousUser(identityId);
                authAccounts = getAuthenticatedPluginAccounts(identityId);
                anonUser = getAnonymousUser(authAccounts);
            }
            boolean hasUserAccounts = hasRegisteredUserAccount(authAccounts);

            if (loggedInUser != null) {
                updateSessionUser(loggedInUser);
            } else if (anonUser != null) {
                updateSessionUser(anonUser);
            }

            currentUserStatus.loggedInUser = loggedInUser;
            currentUserStatus.hasUserAccounts = hasUserAccounts;

            if (currentUserStatus.loggedInUser != null) {
                currentUserStatus.email = currentUserStatus.loggedInUser.email;
            } else {
                currentUserStatus.email = null;
            }
        } catch (Exception e) {
            //
        }

        lastRegisterUserCheck = System.currentTimeMillis();


        return currentUserStatus;
    }

    public void pluginLogout() {
        String api = "/users/plugin/logout";
        SoftwareResponse resp = this.makeApiCall(api, HttpPost.METHOD_NAME, null);

        clearUserStatusCache();

        getUserStatus();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                SessionManager.fetchDailyKpmSessionInfo();
            }
            catch (InterruptedException e){
                System.err.println(e);
            }
        }).start();
    }
    
    protected void lazilyFetchUserStatus(int retryCount) {
        clearUserStatusCache();
        UserStatus userStatus = this.getUserStatus();

        if (userStatus.loggedInUser == null && retryCount > 0) {
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
        String identityId = this.getIdentity();
        String encodedMacAddr = null;
        try {
            encodedMacAddr = URLEncoder.encode(identityId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // url encoding failed, just use the mac addr
            encodedMacAddr = identityId;
        }

        url += "/login?addr=" + encodedMacAddr;
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        new Thread(() -> {
            try {
                Thread.sleep(10000);
                lazilyFetchUserStatus(3);
            }
            catch (InterruptedException e){
                System.err.println(e);
            }
        }).start();
    }

    public void launchSignup() {
        String url = LAUNCH_URL;
        String identityId = this.getIdentity();
        String encodedMacAddr = null;
        try {
            encodedMacAddr = URLEncoder.encode(identityId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // url encoding failed, just use the mac addr
            encodedMacAddr = identityId;
        }
        url += "/onboarding?addr=" + encodedMacAddr;
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }

        new Thread(() -> {
            try {
                Thread.sleep(55000);
                lazilyFetchUserStatus(3);
            }
            catch (InterruptedException e){
                System.err.println(e);
            }
        }).start();
    }

    public void launchWebDashboard() {
        String url = LAUNCH_URL;
        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }
    }
}
