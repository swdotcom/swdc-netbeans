/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin;


import com.swdc.netbeans.plugin.http.SoftwareResponse;
import com.swdc.netbeans.plugin.listeners.DocumentChangeEventListener;
import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.models.KeystrokeData;
import com.swdc.netbeans.plugin.managers.SessionManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
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
    private KeystrokeManager keystrokeMgr = KeystrokeManager.getInstance();
    private static SoftwareUtil softwareUtil = SoftwareUtil.getInstance();
    
    private final int ONE_MINUTE_SECONDS = 60;

    @Override
    public void run() {
        // INFO [Software]: Software.com: Loaded vUnknown on platform: null
        LOG.log(Level.INFO, "Software.com: Loaded v{0}", softwareUtil.getPluginVersion());
        
        // setup the document change event listeners
        setupEventListeners();
        
        // setup the plugin data scheduler (every minute)
        setupScheduledPluginDataProcessor();
        
        // setup the kpm metrics info fetch (every minute)
        setupScheduledKpmMetricsProcessor();
        
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
        ScheduledFuture<?> scheduledFixture = scheduler.scheduleAtFixedRate(
                handler, ONE_MINUTE_SECONDS, ONE_MINUTE_SECONDS, TimeUnit.SECONDS);
    }
    
    private void setupScheduledKpmMetricsProcessor() {
        final Runnable handler = () -> SessionManager.fetchDailyKpmSessionInfo();
        ScheduledFuture<?> scheduledFixture = scheduler.scheduleAtFixedRate(
                handler, 10, ONE_MINUTE_SECONDS, TimeUnit.SECONDS);
    }
    
    private void bootstrapStatus() {
        new Thread(() -> {
            try {
                Thread.sleep(1000 * 15);
                softwareUtil.checkUserAuthenticationStatus();
                softwareUtil.sendOfflineData();
            }
            catch (InterruptedException e){
                LOG.log(Level.WARNING, "Software.com: error boostraping user plugin status, error: {0}", e.getMessage());
            }
        }).start();
    }

    private void processKeystrokes() {
        if(!SoftwareUtil.SEND_TELEMTRY.get()) {
            return;
        }
        
        KeystrokeData keystrokeData = keystrokeMgr.getKeystrokeData();
        if (keystrokeData != null && keystrokeData.hasData()) {
            String payload = SoftwareUtil.gson.toJson(keystrokeData);
            SoftwareResponse resp = softwareUtil.makeApiCall("/data", HttpPost.METHOD_NAME, payload);
            if (!resp.isOk()) {
                // save the data offline
                softwareUtil.storePayload(payload);
            } else {
                keystrokeData.resetData();
            }
        } else if (keystrokeData != null) {
            keystrokeData.resetData();
        }
    }
    
}
