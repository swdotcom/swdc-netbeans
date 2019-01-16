/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.models.KeystrokeData;
import com.swdc.netbeans.plugin.models.KeystrokeMetrics;

/**
 *
 */
public class KeystrokeManager {
    
    private final static Object lock = new Object();
    private static KeystrokeManager instance = null;

    private KeystrokeData keystrokeData = null;
    /**
     * private constructor to defeat instantiation
     */
    private KeystrokeManager() {
        //
    }

    public static KeystrokeManager getInstance() {
        synchronized(lock) {
            if (instance == null) {
                instance = new KeystrokeManager();
            }
        }
        return instance;
    }
    
    public KeystrokeMetrics getKeystrokeMetrics(String fileName, String projectName, String projectDir) {
        synchronized(lock) {
            if (keystrokeData == null) {
                // create the keystroke data structure
                keystrokeData = new KeystrokeData(projectName, projectDir);
            }
        }
        return keystrokeData.getMetrics(fileName);
    }

    public KeystrokeData getKeystrokeData() {
        return keystrokeData;
    }
    
    public void incrementKeystrokes() {
        if (keystrokeData != null) {
            keystrokeData.setKeystrokes(keystrokeData.getKeystrokes() + 1);
        }
    }
    
}
