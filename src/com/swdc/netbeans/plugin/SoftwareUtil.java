/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.swdc.netbeans.plugin.managers.OfflineManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.managers.StatusBarManager;
import com.swdc.netbeans.plugin.models.NetbeansProject;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.apache.commons.lang.StringUtils;
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
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.FileDetails;
import swdc.java.ops.snowplow.entities.UIElementEntity;
import swdc.java.ops.snowplow.events.UIInteractionType;

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
    public static String IDE_NAME = "netbeans";
    public static String IDE_VERSION = "";
    
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
    
    private static final long DAYS_IN_SECONDS = 60 * 60 * 24;
    
    private final static int EOF = -1;
    
    private static String workspace_name = null;
    
    private final static Map<String, String> sessionMap = new HashMap<String, String>();
    
    public static boolean TELEMETRY_ON = true;
    
    private static boolean appAvailable = true;
    private static boolean loggedInCacheState = false;
    private static long lastAppAvailableCheck = 0;
    
    private static boolean showStatusText = true;
    
    private static Document lastDocument = null;
   
    private static String regex = "^\\S+@\\S+\\.\\S+$";
    private static Pattern pattern = Pattern.compile(regex);
    
    static {
        try {
            IDE_VERSION = System.getProperty("netbeans.buildnumber");
        } catch (Exception e) {
            IDE_VERSION = "unknown";
        }
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
        String file = FileUtilManager.getSoftwareSessionFile(false);
        // check if it exists
        File f = new File(file);
        return f.exists();
    }
    
    public static boolean hasJwt() {
        
        String jwt = FileUtilManager.getItem("jwt");
        return (jwt != null && !jwt.equals(""));
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

    
    public static boolean isServerOnline() {
        long nowInSec = Math.round(System.currentTimeMillis() / 1000);
        boolean pastThreshold = (nowInSec - lastAppAvailableCheck > 120);
        if (pastThreshold) {
            ClientResponse resp = OpsHttpClient.softwareGet("/ping", null);
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
        String dashboardFile = FileUtilManager.getCodeTimeDashboardFile();

        String api = "/dashboard?linux=" + UtilManager.isLinux() + "&showToday=true";
        ClientResponse resp = OpsHttpClient.softwareGet(api, FileUtilManager.getItem("jwt"));
            
        String dashboardContent = resp.getJsonStr();

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

        String codeTimeFile = FileUtilManager.getCodeTimeDashboardFile();
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
        String currentDay = FileUtilManager.getItem("currentDay", "");
        String day = getTodayInStandardFormat();
        return !day.equals(currentDay);
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
