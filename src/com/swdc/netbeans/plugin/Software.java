/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;

import com.swdc.netbeans.plugin.listeners.DocumentChangeEventListener;
import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.managers.MusicManager;
import com.swdc.netbeans.plugin.managers.OfflineManager;
import com.swdc.netbeans.plugin.managers.RepoManager;
import com.swdc.netbeans.plugin.models.KeystrokeData;
import com.swdc.netbeans.plugin.managers.SessionManager;
import com.swdc.netbeans.plugin.models.KeystrokeMetrics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
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
    private final SoftwareUtil softwareUtil = SoftwareUtil.getInstance();
    private RepoManager repoManager;
    private MusicManager musicManager;

    private final int ONE_MINUTE_SECONDS = 60;
    private final int ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60;

    private static final int retry_counter = 0;
    private static final long check_online_interval_ms = 1000 * 60 * 10;

    @Override
    public void run() {
        initComponent();
    }

    protected void initComponent() {
        boolean serverIsOnline = softwareUtil.isServerOnline();
        boolean sessionFileExists = softwareUtil.softwareSessionFileExists();
        boolean hasJwt = softwareUtil.hasJwt();
        // no session file or no jwt
        if (!sessionFileExists || !hasJwt) {
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
                String jwt = softwareUtil.createAnonymousUser(serverIsOnline);
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
        musicManager = MusicManager.getInstance();

        // INFO [Software]: Code Time: Loaded vUnknown on platform: null
        LOG.log(Level.INFO, "Code Time: Loaded v{0}", softwareUtil.getVersion());

        // setup the document change event listeners
        setupEventListeners();

        // setup the kpm metrics info fetch (every minute)
        setupScheduledKpmMetricsProcessor();
        
        // setup offline batch processor (every 30 minutes)
        setupOfflineDataSendProcessor();

        setupRepoMusicInfoProcessor();

        final Runnable hourlyJobs = () -> hourlyJobsProcessor();
        int interval = ONE_HOUR_SECONDS;
        scheduler.scheduleAtFixedRate(hourlyJobs, 45, interval, TimeUnit.SECONDS);

        setupUserStatusProcessor();

        // check the user auth status and send any offline data
        bootstrapStatus(initializedUser);
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

    private void setupOfflineDataSendProcessor() {
        final Runnable handler = () -> processOfflineDataSend();
        long everyThrityMin = ONE_MINUTE_SECONDS * 30;
        scheduler.scheduleAtFixedRate(handler, 1 /* 1 second */, everyThrityMin, TimeUnit.SECONDS);
    }

    private void setupScheduledKpmMetricsProcessor() {
        final Runnable handler = () -> SessionManager.fetchDailyKpmSessionInfo(false);
        scheduler.scheduleAtFixedRate(handler, 15, ONE_MINUTE_SECONDS, TimeUnit.SECONDS);
    }

    private void setupUserStatusProcessor() {
        final Runnable handler = () -> processUserStatus();
        scheduler.scheduleAtFixedRate(handler, 60, 90, TimeUnit.SECONDS);
    }

    private void setupRepoMusicInfoProcessor() {
        final Runnable handler = () -> processMusicInfo();
        int interval = 15;
        scheduler.scheduleAtFixedRate(handler, 15, interval, TimeUnit.SECONDS);
    }

    private void hourlyJobsProcessor() {
        // send a heartbeat
        softwareUtil.sendHeartbeat("HOURLY");

        new Thread(() -> {
            try {
                Thread.sleep(1000 * 5);
                processHistoricalCommits();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }).start();
    }

    private void bootstrapStatus(boolean initializedUser) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                initializeUserInfo(initializedUser);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }).start();
    }

    private void processUserStatus() {
        softwareUtil.getUserStatus();
    }

    private void processMusicInfo() {
        musicManager.processMusicTrack();
    }

    private void processHistoricalCommits() {
        KeystrokeData keystrokeData = keystrokeMgr.getKeystrokeData();
        // if we have keystroke data, we'll have the project info
        if (keystrokeData != null && keystrokeData.getProject() != null) {
            String projectDir = keystrokeData.getProject().getDirectory();
            repoManager.getHistoricalCommits(projectDir);
        }
    }

    private void processOfflineDataSend() {
        softwareUtil.sendOfflineData();
        new Thread(() -> {
            try {
                Thread.sleep(1000 * 10);
                SessionManager.fetchDailyKpmSessionInfo(true);
            } catch (Exception e) {
                //
            }
        }).start();
    }

    private void initializeUserInfo(boolean initializedUser) {

        softwareUtil.getUserStatus();

        if (initializedUser) {
            sendInstallPayload();
            // ask the user to login one time only
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    softwareUtil.showLoginPrompt();
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }).start();
        }
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                softwareUtil.sendOfflineData();
                SessionManager.fetchDailyKpmSessionInfo(true);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }).start();

        softwareUtil.sendHeartbeat("INITIALIZED");
    }

    protected void sendInstallPayload() {
        String currentFile = "Untitled";
        String projectName = "Unnamed";
        String projectDir = "";
        KeystrokeMetrics metrics = keystrokeMgr.getKeystrokeMetrics(currentFile, projectName, projectDir);
        metrics.setAdd(1);
        keystrokeMgr.incrementKeystrokes();

        keystrokeMgr.getKeystrokeData().processKeystrokes();
    }

    protected void showOfflinePrompt() {
        String infoMsg = "Our service is temporarily unavailable. We will try to reconnect again "
                + "in 10 minutes. Your status bar will not update at this time.";
        Object[] options = { "OK" };
        JOptionPane.showOptionDialog(null, infoMsg, "Code Time", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }

}
