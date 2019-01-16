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
    private KeystrokeMetrics source;
    
    public KeystrokeFileMetrics(String fileName) {
        this.fileName = fileName;
        this.source = new KeystrokeMetrics();
    }

    public String getFileName() {
        return fileName;
    }

    public KeystrokeMetrics getSource() {
        return source;
    }

    public void setSource(KeystrokeMetrics source) {
        this.source = source;
    }
    
    
}
