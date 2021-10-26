/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.managers;

import swdc.java.ops.model.CodeTime;


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

    public CodeTime getKeystrokeCount() {
        if (wrapper != null) {
            return wrapper.getKeystrokeCount();
        }
        return null;
    }

    public void setKeystrokeCount(String projectName, CodeTime keystrokeCount) {
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
        protected CodeTime keystrokeCount;
        protected String projectName = "";

        public CodeTime getKeystrokeCount() {
            return keystrokeCount;
        }

        public void setKeystrokeCount(CodeTime keystrokeCount) {
            this.keystrokeCount = keystrokeCount;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

    }

}
