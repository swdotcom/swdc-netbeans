/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.models;

/**
 *
 */
public class KeystrokeFileMetrics {
    
    private String fileName;
    private KeystrokeMetrics metrics;
    
    public KeystrokeFileMetrics(String fileName) {
        this.fileName = fileName;
        this.metrics = new KeystrokeMetrics();
    }

    public String getFileName() {
        return fileName;
    }

    public KeystrokeMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(KeystrokeMetrics metrics) {
        this.metrics = metrics;
    }
    
    
}
