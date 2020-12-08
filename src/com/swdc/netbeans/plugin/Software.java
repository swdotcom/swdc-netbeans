/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.swdc.netbeans.plugin.listeners.DocumentChangeEventListener;
import com.swdc.netbeans.plugin.managers.AsyncManager;
import com.swdc.netbeans.plugin.managers.EventTrackerManager;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.managers.SoftwareEventManager;
import com.swdc.netbeans.plugin.managers.StatusBarManager;
import com.swdc.netbeans.plugin.managers.WallClockManager;
import com.swdc.netbeans.plugin.models.KeystrokeCount;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.ModuleInstall;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

/**
 *
 */
@OnShowing
public class Software extends ModuleInstall implements Runnable {

    public static final Logger LOG = Logger.getLogger("Software");

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final int ONE_MINUTE_SECONDS = 60;
    private final int ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60;
    private static final int FOCUS_STATE_INTERVAL_SECONDS = 5;

    private static int retry_counter = 0;
    private static final long check_online_interval_ms = 1000 * 60 * 10;

    @Override
    public void run() {
        String jwt = FileManager.getItem("jwt");
        if (StringUtils.isBlank(jwt)) {
            jwt = SoftwareUtil.createAnonymousUser(false);
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
        
        // initialize the tracker
        EventTrackerManager.getInstance().init();

        // send the activate event
        EventTrackerManager.getInstance().trackEditorAction("editor", "activate");
        
        String readmeDisplayed = FileManager.getItem("netbeans_CtReadme");
        if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
            // send an initial plugin payload
            FileManager.openReadmeFile(UIInteractionType.keyboard);
            FileManager.setItem("netbeans_CtReadme", "true");
        }

        // setup the document change event listeners
        setupEventListeners();

        StatusBarManager.updateStatusBar();
        
        // initialize the wallclock manager
        WallClockManager.getInstance().updateSessionSummaryFromServer();
        
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
                    KeystrokeCount keystrokeCount = KeystrokeManager.getInstance().getKeystrokeCount();
                    if (keystrokeCount != null) {
                        // set the flag the "unfocusStateChangeHandler" will look for in order to process payloads early
                        keystrokeCount.triggered = false;
                        keystrokeCount.processKeystrokes();
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
