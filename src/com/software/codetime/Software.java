/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.software.codetime;

import com.software.codetime.listeners.DocumentChangeEventListener;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.managers.FileManager;
import com.software.codetime.managers.KeystrokeManager;
import com.software.codetime.managers.NetbeansProject;
import com.software.codetime.managers.SessionStatusUpdateManager;
import com.software.codetime.managers.SoftwareEventManager;
import com.software.codetime.managers.SoftwareSessionManager;
import com.software.codetime.managers.ThemeModeInfoManager;
import com.software.codetime.managers.WebsocketMessageManager;
import com.software.codetime.models.KeystrokeCountUtil;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.ModuleInstall;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;
import swdc.java.ops.event.SlackStateChangeObserver;
import swdc.java.ops.event.UserStateChangeObserver;
import swdc.java.ops.manager.AccountManager;
import swdc.java.ops.manager.ConfigManager;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.ConfigOptions;
import swdc.java.ops.snowplow.events.UIInteractionType;
import swdc.java.ops.websockets.WebsocketClient;


@OnShowing
public class Software extends ModuleInstall implements Runnable {

    public static final Logger LOG = Logger.getLogger("Software");

    private final int ONE_MINUTE_SECONDS = 60;
    private final int ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60;
    private static final int FOCUS_STATE_INTERVAL_SECONDS = 5;

    private static final long check_online_interval_ms = 1000 * 60 * 10;
    
    private UserStateChangeObserver userStateChangeObserver;
    private SlackStateChangeObserver slackStateChangeObserver;

    @Override
    public void run() {
        ConfigOptions options = new ConfigOptions();
        options.ideName = SoftwareUtil.IDE_NAME;
        options.pluginType = "codetime";
        options.appUrl = SoftwareUtil.APP_URL;
        options.ideVersion = SoftwareUtil.getVersion();
        options.metricsEndpoint = SoftwareUtil.API_ENDPOINT;
        options.pluginId = SoftwareUtil.PLUGIN_ID;
        options.pluginName = "codetime";
        options.pluginVersion = SoftwareUtil.getVersion();
        options.pluginEditor = "netbans";
        options.softwareDir = SoftwareUtil.SOFTWARE_DIR;
        
        // initialize the swdc ops config
        ConfigManager.init(
                options,
                () -> SoftwareSessionManager.getInstance().refreshSessionDataAndTree(),
                new WebsocketMessageManager(),
                new SessionStatusUpdateManager(),
                new ThemeModeInfoManager(),
                ConfigManager.IdeType.netbeans);
        
        String jwt = FileUtilManager.getItem("jwt");
        if (StringUtils.isBlank(jwt)) {
            jwt = AccountManager.createAnonymousUser(false);
            if (StringUtils.isBlank(jwt)) {
                boolean serverIsOnline = SoftwareUtil.isServerOnline();
                if (!serverIsOnline) {
                    showOfflinePrompt();
                }
            } else {
                initializePlugin(true);
            }
        }
        initializePlugin(false);
    }

    protected void initializePlugin(boolean initializedUser) {

        // INFO [Software]: Code Time: Loaded vUnknown on platform: null
        LOG.log(Level.INFO, "Code Time: Loaded v{0}", SoftwareUtil.getVersion());
        
        try {
            WebsocketClient.connect();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error connecting websocket channel");
        }
        
        // initialize the tracker
        EventTrackerManager.getInstance().init(new NetbeansProject());

        // send the activate event
        EventTrackerManager.getInstance().trackEditorAction("editor", "activate");
        
        String readmeDisplayed = FileUtilManager.getItem("netbeans_CtReadme");
        if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
            // send an initial plugin payload
            FileManager.openReadmeFile(UIInteractionType.keyboard);
            FileUtilManager.setItem("netbeans_CtReadme", "true");
        }

        // setup the document change event listeners
        setupEventListeners();

        SoftwareSessionManager.getInstance().refreshSessionDataAndTree();
        
        final Runnable checkFocusStateTimer = () -> checkFocusState();
        AsyncManager.getInstance().scheduleService(
                checkFocusStateTimer, "checkFocusStateTimer", 0, FOCUS_STATE_INTERVAL_SECONDS);
       
    }

    /**
     * Add the file change listener
     */
    private void setupEventListeners() {
        PropertyChangeListener l = (PropertyChangeEvent evt) -> {
            JTextComponent jtc = EditorRegistry.focusedComponent();
            if (jtc != null && jtc.isShowing()) {
                Document d = jtc.getDocument();
                DocumentChangeEventListener listener = new DocumentChangeEventListener(d);
                d.addDocumentListener(listener);
                listener.update();
            }
        };

        EditorRegistry.addPropertyChangeListener(l);
    }

    protected void showOfflinePrompt() {
        String infoMsg = "Our service is temporarily unavailable. We will try to reconnect again "
                + "in 10 minutes. Your status bar will not update at this time.";
        Object[] options = { "OK" };
        JOptionPane.showOptionDialog(null, infoMsg, "Code Time", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }
    
    private void checkFocusState() {
        SwingUtilities.invokeLater(() -> {
            boolean isActive = WindowManager.getDefault().getMainWindow().isFocused();
            
            boolean focusStateChanged = isActive != SoftwareEventManager.isCurrentlyActive;
            if (focusStateChanged) {
                if (!isActive) {
                    CodeTime keystrokeCount = KeystrokeManager.getInstance().getKeystrokeCount();
                    if (keystrokeCount != null && keystrokeCount.hasData()) {
                        // set the flag the "unfocusStateChangeHandler" will look for in order to process payloads early
                        KeystrokeCountUtil.preProcessKeystrokeData(keystrokeCount, check_online_interval_ms, check_online_interval_ms);
                    }
                    EventTrackerManager.getInstance().trackEditorAction("editor", "unfocus");
                } else {
                    // just set the process keystrokes payload to false since we're focused again
                    EventTrackerManager.getInstance().trackEditorAction("editor", "focus");
                }
            }
            
            // update the currently active flag
            SoftwareEventManager.isCurrentlyActive = isActive;
        });
    }

}