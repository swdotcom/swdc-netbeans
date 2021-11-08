/*
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.software.codetime.listeners;

import com.software.codetime.managers.SoftwareEventManager;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

/**
 *
 */
public class DocumentChangeEventListener implements DocumentListener {
    public static final Logger log = Logger.getLogger("DocumentChangeEventListener");
    
    private final Document document;
    
    private final SoftwareEventManager eventMgr = SoftwareEventManager.getInstance();

    public DocumentChangeEventListener(Document d) {
        this.document = d;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        this.handleTyping(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.handleTyping(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.handleTyping(e);
    }

    public void update() {
        //
    }

    public void remove() {
        this.document.removeDocumentListener(this);
    }

    public void handleTyping(final DocumentEvent e) {
        eventMgr.handleChangeEvents(e);
    }
    
}
