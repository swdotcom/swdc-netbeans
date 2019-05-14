/*
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.listeners;

import com.swdc.netbeans.plugin.managers.KeystrokeManager;
import com.swdc.netbeans.plugin.models.KeystrokeMetrics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;

/**
 *
 */
public class DocumentChangeEventListener implements DocumentListener {
    public static final Logger log = Logger.getLogger("DocumentChangeEventListener");
    
    private final Document document;
    
    private KeystrokeManager keystrokeMgr = KeystrokeManager.getInstance();

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
        final FileObject file = this.getFile();
        if (file != null) {
            final Project currentProject = this.getProject();
            if (currentProject == null) {
                return;
            }
            final String projectName = currentProject.getProjectDirectory().getName();
            final String projectDir = currentProject.getProjectDirectory().getPath();
            final String currentFile = file.getPath();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // we need currentFile, projectName, and projectDir
                    KeystrokeMetrics metrics = keystrokeMgr.getKeystrokeMetrics(currentFile, projectName, projectDir);
                    
                    DocumentEvent.ElementChange lineChange = e.getChange(e.getDocument().getDefaultRootElement());
                    
                    if (metrics.getLength() == 0) {
                        // set the length
                        metrics.setLength(e.getDocument().getLength());
                    }
                    
                    if (metrics.getLines() == 0) {
                        int lineCount = getLineCount(currentFile);
                        metrics.setLines(lineCount);
                    }
                    
                    if (metrics.getSyntax() == null || metrics.getSyntax().equals("")) {
                        if (currentFile.contains(".")) {
                            String ext = currentFile.substring(currentFile.lastIndexOf(".") + 1);
                            metrics.setSyntax(ext);
                        }
                    }
                    

                    if (lineChange != null) {
                        // check if it's line removed or added
                        Element[] addedLines = lineChange.getChildrenAdded();
                        Element[] removedLines = lineChange.getChildrenRemoved();
                        if (addedLines != null && addedLines.length > 0) {
                            metrics.setLinesAdded(metrics.getLinesAdded() + 1);
                            keystrokeMgr.incrementKeystrokes();
                            return;
                        } else if (removedLines != null && removedLines.length > 0) {
                            metrics.setLinesRemoved(metrics.getLinesRemoved() + 1);
                            keystrokeMgr.incrementKeystrokes();
                            return;
                        }
                    }
                    
                    if (e.getType() == DocumentEvent.EventType.INSERT) {
                        if (e.getLength() > 5) {
                            // paste
                            metrics.setPaste(metrics.getPaste() + 1);
                        } else {
                            // add
                            metrics.setAdd(metrics.getAdd() + 1);
                        }
                        keystrokeMgr.incrementKeystrokes();
                    } else if (e.getType() == DocumentEvent.EventType.REMOVE) {
                        metrics.setDelete(metrics.getDelete() + 1);
                        keystrokeMgr.incrementKeystrokes();
                    } else {
                        log.log(Level.INFO, "Change event happened: {0}", e.toString());
                    }
                    
                }
            });
        }
    }
    
    private int getLineCount(String fileName) {
        try {
            Path path = Paths.get(fileName);
            Stream<String> stream = Files.lines(path);
            int count = (int) stream.count();
            try {
                stream.close();
            } catch (Exception e) {
                //
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private FileObject getFile() {
        if (this.document == null)
            return null;
        Source source = Source.create(this.document);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return fileObject;
    }

    private Project getProject() {
        if (this.document == null)
            return null;
        Source source = Source.create(document);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return FileOwnerQuery.getOwner(fileObject);
    }
    
}
