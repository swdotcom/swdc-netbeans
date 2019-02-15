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
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
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

    public static JsonParser jsonParser = new JsonParser();
    public static Gson gson = new Gson();

    private static boolean confirmWindowOpen = false;
    
    public static boolean TELEMETRY_ON = true;
    
    private SoftwareStatusBar statusBar;

    public static SoftwareUtil getInstance() {
        synchronized (UTIL_LOCK) {
            if (instance == null) {
                instance = new SoftwareUtil();
                
            }
        }
        return instance;
    }
    
    private SoftwareUtil() {
        statusBar = new SoftwareStatusBar();
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
        if (jsonObj != null && jsonObj.has(key)) {
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
            file += "\\CodeTime";
        } else {
            file += "/CodeTime";
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
        
        SoftwareResponse softwareResponse = new SoftwareResponse();
        if (!TELEMETRY_ON) {
            softwareResponse.setIsOk(true);
            return softwareResponse;
        }

        SoftwareHttpManager httpTask = new SoftwareHttpManager(api, httpMethodName, payload);
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

    public boolean isPastTimeThreshold() {
        String lastUpdateTimeStr = getItem("netbeans_lastUpdateTime");
        Long lastUpdateTime = (lastUpdateTimeStr != null) ? Long.valueOf(lastUpdateTimeStr) : null;
        return !(lastUpdateTime != null
                && System.currentTimeMillis() - lastUpdateTime < MILLIS_PER_HOUR * LONG_THRESHOLD_HOURS);
    }

    public void checkTokenAvailability() {
        String tokenVal = getItem("token");

        if (tokenVal == null || tokenVal.equals("")) {
            return;
        }

        SoftwareResponse resp = makeApiCall("/users/plugin/confirm?token=" + tokenVal, HttpGet.METHOD_NAME, null);
        JsonObject responseData = resp.getJsonObj();
        if (responseData != null) {
            // update the jwt, user and netbeans_lastUpdateTime
            setItem("jwt", responseData.get("jwt").getAsString());
            setItem("user", responseData.get("user").getAsString());
            setItem("netbeans_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
        } else if (!resp.isDeactivated()) {
            // check again in 2 minutes
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 120);
                    checkTokenAvailability();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
        }
    }

    private boolean isServerOnline() {
        return makeApiCall("/ping", HttpGet.METHOD_NAME, null).isOk();
    }

    public void checkUserAuthenticationStatus() {
        
        if (!TELEMETRY_ON) {
            return;
        }

        boolean isOnline = isServerOnline();
        boolean authenticated = isAuthenticated();
        boolean pastThresholdTime = isPastTimeThreshold();

        boolean requiresLogin = (isOnline && !authenticated && pastThresholdTime && !confirmWindowOpen);

        if (requiresLogin) {
            setItem("netbeans_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
            confirmWindowOpen = true;
            String msg = "To see your coding data in Code Time, please log in to your account.";

            Object[] options = {"Log in", "Not now"};
            int choice = JOptionPane.showOptionDialog(
                    null, msg, "Software", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            // interpret the user's choice
            if (choice == JOptionPane.YES_OPTION) {
                launchDashboard();
            }
        } else if (requiresLogin) {
            // try again in 25 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 25);
                    checkUserAuthenticationStatus();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
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

    public void launchDashboard() {

        String url = LAUNCH_URL;

        // create the token value
        String token = getItem("token");
        String jwt = getItem("jwt");
        boolean addToken = false;
        if (token == null || token.equals("")) {
            token = generateToken();
            setItem("token", token);
            addToken = true;
        } else if (jwt == null || jwt.equals("") || !isAuthenticated()) {
            addToken = true;
        }
        if (addToken) {
            url += "/onboarding?token=" + token;

            // checkTokenAvailability in a minute
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * 60);
                    checkTokenAvailability();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
        }

        try {
            URL launchUrl = new URL(url);
            URLDisplayer.getDefault().showURL(launchUrl);
        } catch (MalformedURLException e) {
            LOG.log(Level.WARNING, "Failed to launch the url: {0}, error: {1}", new Object[]{url, e.getMessage()});
        }
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
    
    public void launchCodeTimeMetricsDashboard() {
        String api = "/dashboard";
        String dashboardContent = this.makeApiCall(api, HttpGet.METHOD_NAME, null).getJsonStr();
        String codeTimeFile = this.getCodeTimeDashboardFile();
        File f = new File(codeTimeFile);

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
        
        try {
            // open the file in the editor
            FileObject fo = FileUtil.createData(f);
            DataObject d = DataObject.find(fo);
            NbDocument.openDocument(d, PLUGIN_ID, Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }

    public void setStatusLineMessage(final StatusBarType barType, final String statusMsg, final String tooltip) {
        statusBar.updateMessage(barType, statusMsg, tooltip);
    }
}
