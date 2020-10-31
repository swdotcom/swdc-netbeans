/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.swdc.netbeans.plugin.listeners.DocumentChangeEventListener;
import com.swdc.netbeans.plugin.managers.EventTrackerManager;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.managers.RepoManager;
import com.swdc.netbeans.plugin.managers.StatusBarManager;
import com.swdc.netbeans.plugin.managers.WallClockManager;
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
import org.netbeans.api.editor.EditorRegistry;
import org.openide.modules.ModuleInstall;
import org.openide.windows.OnShowing;

/**
 *
 */
@OnShowing
public class Software extends ModuleInstall implements Runnable {

    public static final Logger LOG = Logger.getLogger("Software");

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private KeystrokeManager keystrokeMgr;
    private RepoManager repoManager;

    private final int ONE_MINUTE_SECONDS = 60;
    private final int ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60;

    private static final int retry_counter = 0;
    private static final long check_online_interval_ms = 1000 * 60 * 10;

    @Override
    public void run() {
        initComponent();
    }

    protected void initComponent() {
        boolean serverIsOnline = SoftwareUtil.isServerOnline();
        boolean hasJwt = SoftwareUtil.hasJwt();
        // no session file or no jwt
        if (!hasJwt) {
            if (!serverIsOnline) {
                // server isn't online, check again in 10 min
                if (retry_counter == 0) {
                    showOfflinePrompt();
                }
                new Thread(() -> {
                    try {
                        Thread.sleep(check_online_interval_ms);
                        initComponent();
                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }
                }).start();
            } else {
                // create the anon user
                String jwt = SoftwareUtil.createAnonymousUser(serverIsOnline);
                if (jwt == null) {
                    // it failed, try again later
                    if (retry_counter == 0) {
                        initComponent();
                    }
                    new Thread(() -> {
                        try {
                            Thread.sleep(check_online_interval_ms);
                            initComponent();
                        } catch (InterruptedException e) {
                            System.err.println(e);
                        }
                    }).start();
                } else {
                    initializePlugin(true);
                }
            }
        } else {
            // session json already exists, continue with plugin init
            initializePlugin(false);
        }
    }

    protected void initializePlugin(boolean initializedUser) {
        keystrokeMgr = KeystrokeManager.getInstance();
        repoManager = RepoManager.getInstance();

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

        // send the init heartbeat
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(5000);
                SoftwareUtil.sendHeartbeat("INITIALIZED");
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        });
        
        // initialize the wallclock manager
        WallClockManager.getInstance().updateSessionSummaryFromServer();
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

}
