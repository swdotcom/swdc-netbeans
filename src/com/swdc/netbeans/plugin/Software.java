/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;


import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.listeners.DocumentChangeEventListener;
import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.managers.MusicManager;
import com.swdc.netbeans.plugin.managers.RepoManager;
import com.swdc.netbeans.plugin.models.KeystrokeData;
import com.swdc.netbeans.plugin.managers.SessionManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
    private SoftwareUtil softwareUtil;
    private RepoManager repoManager;
    private MusicManager musicManager;
    
    private final int ONE_MINUTE_SECONDS = 60;
    private final int ONE_HOUR_SECONDS = ONE_MINUTE_SECONDS * 60;

    @Override
    public void run() {
        keystrokeMgr = KeystrokeManager.getInstance();
        softwareUtil = SoftwareUtil.getInstance();
        repoManager = RepoManager.getInstance();
        musicManager = MusicManager.getInstance();
        
        // INFO [Software]: Code Time: Loaded vUnknown on platform: null
        LOG.log(Level.INFO, "Code Time: Loaded v{0}", softwareUtil.getPluginVersion());
        
        // setup the document change event listeners
        setupEventListeners();
        
        // setup the plugin data scheduler (every minute)
        setupScheduledPluginDataProcessor();
        
        // setup the kpm metrics info fetch (every minute)
        setupScheduledKpmMetricsProcessor();
        
        setupRepoMusicInfoProcessor();
        
        setupRepoCommitsProcessor();
        
        setupRepoMembersProcessor();
        
        setupUserStatusProcessor();
        
        // check the user auth status and send any offline data
        bootstrapStatus();
    }
    
    /**
     * Add the file change listener
     */
    private void setupEventListeners() {
        PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                JTextComponent jtc = EditorRegistry.lastFocusedComponent();
                if (jtc != null) {
                    Document d = jtc.getDocument();
                    DocumentChangeEventListener listener = new DocumentChangeEventListener(d);
                    d.addDocumentListener(listener);
                    listener.update();
                }
            }
        };

        EditorRegistry.addPropertyChangeListener(l);
    }
    
    private void setupScheduledPluginDataProcessor() {
        final Runnable handler = () -> processKeystrokes();
        scheduler.scheduleAtFixedRate(
                handler, ONE_MINUTE_SECONDS, ONE_MINUTE_SECONDS, TimeUnit.SECONDS);
    }
    
    private void setupScheduledKpmMetricsProcessor() {
        final Runnable handler = () -> SessionManager.fetchDailyKpmSessionInfo();
        scheduler.scheduleAtFixedRate(
                handler, 10, ONE_MINUTE_SECONDS, TimeUnit.SECONDS);
    }
    
    private void setupUserStatusProcessor() {
        final Runnable handler = () -> processUserStatus();
        scheduler.scheduleAtFixedRate(
                handler, 60, 90, TimeUnit.SECONDS);
    }
    
    private void setupRepoMusicInfoProcessor() {
        final Runnable handler = () -> processMusicInfo();
        int interval = 15;
        scheduler.scheduleAtFixedRate(
                handler, 15, interval, TimeUnit.SECONDS);
    }
    
    private void setupRepoCommitsProcessor() {
        final Runnable handler = () -> processHistoricalCommits();
        int interval = ONE_HOUR_SECONDS + 20;
        scheduler.scheduleAtFixedRate(
                handler, 45, interval, TimeUnit.SECONDS);
    }
    
    private void setupRepoMembersProcessor() {
        final Runnable handler = () -> processRepoMembers();
        int interval = ONE_HOUR_SECONDS + 10;
        scheduler.scheduleAtFixedRate(
                handler, 35, interval, TimeUnit.SECONDS);
    }
    
    private void bootstrapStatus() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                initializeUserInfo();
                softwareUtil.sendOfflineData();
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
        } else {
            // try again in 2 minutes
            final Runnable handler = () -> processHistoricalCommits();
            scheduler.schedule(handler, (ONE_MINUTE_SECONDS * 2), TimeUnit.SECONDS);
        }
    }
    
    private void processRepoMembers() {
        KeystrokeData keystrokeData = keystrokeMgr.getKeystrokeData();
        // if we have keystroke data, we'll have the project info
        if (keystrokeData != null && keystrokeData.getProject() != null) {
            String projectDir = keystrokeData.getProject().getDirectory();
            repoManager.processRepoMembersInfo(projectDir);
        } else {
            // try again in 2 minutes
            final Runnable handler = () -> processRepoMembers();
            scheduler.schedule(handler, (ONE_MINUTE_SECONDS * 2), TimeUnit.SECONDS);
        }
    }

    private void processKeystrokes() {
        if(!SoftwareUtil.SEND_TELEMTRY.get()) {
            return;
        }
        
        KeystrokeData keystrokeData = keystrokeMgr.getKeystrokeData();
        if (keystrokeData != null && keystrokeData.hasData()) {
            String payload = keystrokeData.getPayload();
            SoftwareResponse resp = softwareUtil.makeApiCall("/data", HttpPost.METHOD_NAME, payload);
            if (!resp.isOk()) {
                // save the data offline
                softwareUtil.storePayload(payload);
            }
            keystrokeMgr.reset();
        } else if (keystrokeData != null) {
            keystrokeMgr.reset();
        }
    }
    
    private void initializeUserInfo() {
        new Thread(() -> {
            SoftwareUtil.UserStatus userStatus = softwareUtil.getUserStatus();
            if (userStatus.loggedInUser == null) {
                // ask the user to login one time only
                // run the initial calls in 6 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(1000 * 10);
                        softwareUtil.checkUserAuthenticationStatus();
                    }
                    catch (Exception e){
                        System.err.println(e);
                    }
                }).start();
                initializeCalls();
            }
        }).start();
    }
    
    private void initializeCalls() {
        new Thread(() -> {
            softwareUtil.sendOfflineData();
            SessionManager.fetchDailyKpmSessionInfo();
        }).start();
    }
    
}
