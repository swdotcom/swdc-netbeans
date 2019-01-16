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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.awt.StatusDisplayer;

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
    public final static String api_endpoint = PROD_API_ENDPOINT;
    // set the launch url to use
    public final static String launch_url = PROD_URL_ENDPOINT;
    // netbeans plugin id
    public final static int pluginId = 11;

    private final static int EOF = -1;

    public static final AtomicBoolean SEND_TELEMTRY = new AtomicBoolean(true);

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public static JsonParser jsonParser = new JsonParser();
    public static Gson gson = new Gson();

    private static boolean confirmWindowOpen = false;

    public static SoftwareUtil getInstance() {
        synchronized (UTIL_LOCK) {
            if (instance == null) {
                instance = new SoftwareUtil();
            }
        }
        return instance;
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
            Writer output = new BufferedWriter(new FileWriter(sessionFile));
            output.write(content);
            output.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Software.com: Failed to write the key value pair ({0}, {1}) into the session, error: {2}", new Object[]{key, val, e.getMessage()});
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
                LOG.log(Level.WARNING, "Software.com: Error trying to read and json parse the session file.{0}", e.getMessage());
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
            LOG.log(Level.WARNING, "Software.com: Error appending to the Software data store file, error: {0}", e.getMessage());
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

    private String getStringRepresentation(HttpEntity res) throws IOException {
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

        BufferedReader br = new BufferedReader(reader);
        boolean done = false;
        while (!done) {
            String aLine = br.readLine();
            if (aLine != null) {
                sb.append(aLine);
            } else {
                done = true;
            }
        }
        br.close();

        return sb.toString();
    }

    public SoftwareResponse makeApiCall(String api, String httpMethodName, String payload) {

        SoftwareHttpManager httpTask = new SoftwareHttpManager(api, httpMethodName, payload);
        Future<HttpResponse> response = executorService.submit(httpTask);

        SoftwareResponse softwareResponse = new SoftwareResponse();

        //
        // Handle the Future if it exist
        //
        if (response != null) {
            try {
                HttpResponse httpResponse = response.get();
                if (httpResponse != null) {
                    if (httpResponse.getStatusLine().getStatusCode() < 300) {
                        softwareResponse.setIsOk(true);
                    }
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity != null) {
                        try {
                            String jsonStr = getStringRepresentation(entity);
                            softwareResponse.setJsonStr(jsonStr);
                            LOG.log(Level.INFO, "Sofware.com: API response {0}", jsonStr);
                            if (jsonStr != null) {
                                Object jsonEl = jsonParser.parse(jsonStr);
                                if (jsonEl instanceof JsonElement) {
                                    JsonObject jsonObj = ((JsonElement) jsonEl).getAsJsonObject();
                                    softwareResponse.setJsonObj(jsonObj);
                                }
                            }
                        } catch (IOException e) {
                            String errorMessage = "Software.com: Unable to get the response from the http request, error: " + e.getMessage();
                            softwareResponse.setErrorMessage(errorMessage);
                            LOG.log(Level.WARNING, errorMessage);
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                String errorMessage = "Software.com: Unable to get the response from the http request, error: " + e.getMessage();
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

                //Construct BufferedReader from InputStreamReader
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));

                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        sb.append(line).append(",");
                    }
                }

                br.close();

                if (sb.length() > 0) {
                    String payloads = sb.toString();
                    payloads = payloads.substring(0, payloads.lastIndexOf(","));
                    payloads = "[" + payloads + "]";
                    if (makeApiCall("/data/batch", HttpPost.METHOD_NAME, payloads).isOk()) {
                        deleteFile(dataStoreFile);
                    }
                } else {
                    LOG.log(Level.INFO, "Software.com: No offline data to send");
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Software.com: Error trying to read and send offline data, error: {0}", e.getMessage());
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

        JsonObject responseData = makeApiCall("/users/plugin/confirm?token=" + tokenVal, HttpGet.METHOD_NAME, null).getJsonObj();
        if (responseData != null) {
            // update the jwt, user and netbeans_lastUpdateTime
            setItem("jwt", responseData.get("jwt").getAsString());
            setItem("user", responseData.get("user").getAsString());
            setItem("netbeans_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
        } else {
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

        boolean isOnline = isServerOnline();
        boolean authenticated = isAuthenticated();
        boolean pastThresholdTime = isPastTimeThreshold();

        boolean requiresLogin = (isOnline && !authenticated && pastThresholdTime && !confirmWindowOpen);

        if (requiresLogin) {
            setItem("netbeans_lastUpdateTime", String.valueOf(System.currentTimeMillis()));
            confirmWindowOpen = true;
            String msg = "To see your coding data in Software.com, please log in to your account.";

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

        boolean isOk = makeApiCall("/users/ping/", HttpGet.METHOD_NAME, null).isOk();
        if (!isOk) {
            // update the status bar with Sign Up message
            setStatusLineMessage("⚠️Software.com", "Click to log in to Software.com");
        }
        return isOk;
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

    /**
     * Execute the args
     *
     * @param args
     * @return
     */
public String runCommand(String[] args, String dir) {
        // use process builder as it allows to run the command from a specified dir
        ProcessBuilder builder = new ProcessBuilder();

        try {
            builder.command(args);
            if (dir != null) {
                // change to the directory to run the command
                builder.directory(new File(dir));
            }
            Process process = builder.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream is = process.getInputStream();
            copyLarge(is, baos, new byte[4096]);
            return baos.toString().trim();

        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public void launchDashboard() {

        String url = launch_url;

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

    public void setStatusLineMessage(final String statusMsg, final String tooltip) {
        StatusDisplayer.getDefault().setStatusText(statusMsg);
    }
}
