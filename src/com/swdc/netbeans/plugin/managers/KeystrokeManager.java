/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.models.KeystrokeCount;


public class KeystrokeManager {

    private static KeystrokeManager instance = null;

    KeystrokeCountWrapper wrapper = new KeystrokeCountWrapper();

    /**
     * Protected constructor to defeat instantiation
     */
    protected KeystrokeManager() {
        //
    }

    public static KeystrokeManager getInstance() {
        if (instance == null) {
            instance = new KeystrokeManager();
        }
        return instance;
    }

    public KeystrokeCount getKeystrokeCount() {
        if (wrapper != null) {
            return wrapper.getKeystrokeCount();
        }
        return null;
    }

    public void setKeystrokeCount(String projectName, KeystrokeCount keystrokeCount) {
        if (wrapper == null) {
            wrapper = new KeystrokeCountWrapper();
        }
        wrapper.setKeystrokeCount(keystrokeCount);
        wrapper.setProjectName(projectName);
    }

    public KeystrokeCountWrapper getKeystrokeWrapper() {
        return wrapper;
    }

    public class KeystrokeCountWrapper {
        // KeystrokeCount cache metadata
        protected KeystrokeCount keystrokeCount;
        protected String projectName = "";

        public KeystrokeCount getKeystrokeCount() {
            return keystrokeCount;
        }

        public void setKeystrokeCount(KeystrokeCount keystrokeCount) {
            this.keystrokeCount = keystrokeCount;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

    }

}
