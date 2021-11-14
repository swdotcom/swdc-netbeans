/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.managers;


import com.software.codetime.SoftwareUtil;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.Project;

public class SoftwareEventManager {

    public static final Logger LOG = Logger.getLogger("SoftwareEventManager");

    private static SoftwareEventManager instance = null;

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");
    private static final Pattern NEW_LINE_TAB_PATTERN = Pattern.compile("\n\t");
    private static final Pattern TAB_PATTERN = Pattern.compile("\t");

    public static boolean isCurrentlyActive = true;

    private EventTrackerManager tracker;
    private KeystrokeManager keystrokeMgr;
    private Document lastDocument;

    public static SoftwareEventManager getInstance() {
        if (instance == null) {
            instance = new SoftwareEventManager();
        }
        return instance;
    }

    private SoftwareEventManager() {
        keystrokeMgr = KeystrokeManager.getInstance();
        tracker = EventTrackerManager.getInstance();
    }

    private int getNewlineCount(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = NEW_LINE_PATTERN.matcher(text);
        int count = 0;
        while(matcher.find()) {
            count++;
        }
        return count;
    }

    private CodeTime getCurrentKeystrokeCount(org.netbeans.api.project.Project proj) {
        CodeTime keystrokeCount = keystrokeMgr.getKeystrokeCount();
        if (keystrokeCount == null) {
            // create one
            String projectName = proj != null ? proj.getProjectDirectory().getName() : UtilManager.unnamed_project_name;
            String projectDir = proj != null  ? proj.getProjectDirectory().getPath() : UtilManager.untitled_file_name;
            // create the keysrtroke count wrapper
            createKeystrokeCountWrapper(projectName, projectDir);

            // now retrieve it from the mgr
            keystrokeCount = keystrokeMgr.getKeystrokeCount();
        }
        return keystrokeCount;
    }

    private void updateFileInfoMetrics(DocumentEvent e, CodeTime.FileInfo fileInfo, CodeTime keystrokeCount) {
        int changeLength = e.getLength();

        ElementChange lineChange = e.getChange(lastDocument.getDefaultRootElement());

        int linesAdded = 0;
        int linesRemoved = 0;
        if (lineChange != null) {
            // check if it's line removed or added
            Element[] addedLines = lineChange.getChildrenAdded();
            Element[] removedLines = lineChange.getChildrenRemoved();
            if (addedLines != null && addedLines.length > 0) {
                linesAdded = addedLines.length;
            } else if (removedLines != null && removedLines.length > 0) {
                linesRemoved = removedLines.length;
            }
        }
        
        String text = DocumentUtilities.getModificationText(e);
        
        EventType etype = e.getType();
        int numKeystrokes = 0;
        int numDeleteKeystrokes = 0;
        if (etype.equals(EventType.INSERT)) {
            // characters have been added
            numKeystrokes = changeLength;
        } else if (etype.equals(EventType.REMOVE)) {
            // characters have been removed
            numDeleteKeystrokes = changeLength;
        }

        // check if its an auto indent
        boolean hasAutoIndent = text.matches("^\\s{2,4}$") || TAB_PATTERN.matcher(text).find();
        boolean newLineAutoIndent = text.matches("^\n\\s{2,4}$") || NEW_LINE_TAB_PATTERN.matcher(text).find();


        // event updates
        if (newLineAutoIndent) {
            // it's a new line with auto-indent
            fileInfo.auto_indents += 1;
            fileInfo.linesAdded += 1;
        } else if (hasAutoIndent) {
            // it's an auto indent action
            fileInfo.auto_indents += 1;
        } else if (linesAdded == 1) {
            // it's a single new line action (single_adds)
            fileInfo.single_adds += 1;
            fileInfo.linesAdded += 1;
        } else if (linesAdded > 1) {
            // it's a multi line paste action (multi_adds)
            fileInfo.linesAdded += linesAdded;
            fileInfo.paste += 1;
            fileInfo.multi_adds += 1;
            fileInfo.is_net_change = true;
            fileInfo.characters_added += Math.abs(numKeystrokes - linesAdded);
        } else if (numDeleteKeystrokes > 0 && numKeystrokes > 0) {
            // it's a replacement
            fileInfo.replacements += 1;
            fileInfo.characters_added += numKeystrokes;
            fileInfo.characters_deleted += numDeleteKeystrokes;
        } else if (numKeystrokes > 1) {
            // pasted characters (multi_adds)
            fileInfo.paste += 1;
            fileInfo.multi_adds += 1;
            fileInfo.is_net_change = true;
            fileInfo.characters_added += numKeystrokes;
        } else if (numKeystrokes == 1) {
            // it's a single keystroke action (single_adds)
            fileInfo.add += 1;
            fileInfo.single_adds += 1;
            fileInfo.characters_added += 1;
        } else if (linesRemoved == 1) {
            // it's a single line deletion
            fileInfo.linesRemoved += 1;
            fileInfo.single_deletes += 1;
            fileInfo.characters_deleted += numDeleteKeystrokes;
        } else if (linesRemoved > 1) {
            // it's a multi line deletion and may contain characters
            fileInfo.characters_deleted += numDeleteKeystrokes;
            fileInfo.multi_deletes += 1;
            fileInfo.is_net_change = true;
            fileInfo.linesRemoved += linesRemoved;
        } else if (numDeleteKeystrokes == 1) {
            // it's a single character deletion action
            fileInfo.delete += 1;
            fileInfo.single_deletes += 1;
            fileInfo.characters_deleted += 1;
        } else if (numDeleteKeystrokes > 1) {
            // it's a multi character deletion action
            fileInfo.multi_deletes += 1;
            fileInfo.is_net_change = true;
            fileInfo.characters_deleted += numDeleteKeystrokes;
        }

        fileInfo.keystrokes += 1;
        keystrokeCount.keystrokes += 1;
    }

    // this is used to close unended files
    public void handleSelectionChangedEvents(String fileName, org.netbeans.api.project.Project project) {
        CodeTime keystrokeCount = getCurrentKeystrokeCount(project);

        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        keystrokeCount.endPreviousModifiedFiles(fileName);
    }

    public void handleFileOpenedEvents(String fileName, org.netbeans.api.project.Project project) {
        
        CodeTime keystrokeCount = getCurrentKeystrokeCount(project);

        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        keystrokeCount.endPreviousModifiedFiles(fileName);
        fileInfo.open = fileInfo.open + 1;
        int documentLineCount = SoftwareUtil.getLineCount(fileName);
        fileInfo.lines = documentLineCount;
        LOG.info("Code Time: file opened: " + fileName);
        tracker.trackEditorAction("file", "open", fileName);
    }

    public void handleFileClosedEvents(String fileName, org.netbeans.api.project.Project project) {
        CodeTime keystrokeCount = getCurrentKeystrokeCount(project);
        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        fileInfo.close = fileInfo.close + 1;
        LOG.info("Code Time: file closed: " + fileName);
        tracker.trackEditorAction("file", "close", fileName);
    }

    /**
     * Handles character change events in a file
     * @param document
     * @param documentEvent
     */
    public void handleChangeEvents(DocumentEvent e) {
        lastDocument = e.getDocument();
        if (lastDocument == null) {
            return;
        }
        
        FileObject fileObj = getFile();
        // check whether it's a code time file or not
        // .*\.software.*(data\.json|session\.json|latestKeystrokes\.json|ProjectContributorCodeSummary\.txt|CodeTime\.txt|SummaryInfo\.txt|events\.json|fileChangeSummary\.json)
        boolean skip = (fileObj == null || StringUtils.isBlank(fileObj.getPath()) || fileObj.getPath().matches(".*\\.software.*(data\\.json|session\\.json|latestKeystrokes\\.json|ProjectContributorCodeSummary\\.txt|CodeTime\\.txt|SummaryInfo\\.txt|events\\.json|fileChangeSummary\\.json)"));
        if (skip) {
            return;
        }
        
        String fileName = fileObj.getPath();
        
        org.netbeans.api.project.Project p = getProject();
        CodeTime keystrokeCount = getCurrentKeystrokeCount(p);
        
        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (StringUtils.isBlank(fileInfo.syntax)) {
            // get the grammar
            fileInfo.syntax = DocumentUtilities.getMimeType(lastDocument);
        }
        
        fileInfo.lines = SoftwareUtil.getLineCount(fileName);
        fileInfo.length = lastDocument.getLength();
        
        updateFileInfoMetrics(e, fileInfo, keystrokeCount);
    }

    public void createKeystrokeCountWrapper(String projectName, String projectFilepath) {
        //
        // Create one since it hasn't been created yet
        // and set the start time (in seconds)
        //
        CodeTime keystrokeCount = new CodeTime();

        Project keystrokeProject = new Project(projectName, projectFilepath);
        keystrokeCount.setProject( keystrokeProject );

        //
        // Update the manager with the newly created KeystrokeCount object
        //
        keystrokeMgr.setKeystrokeCount(projectName, keystrokeCount);
    }


    private String getProjectDirectory(String projectName, String fileName) {
        String projectDirectory = "";
        if ( projectName != null && projectName.length() > 0 &&
                fileName != null && fileName.length() > 0 &&
                fileName.indexOf(projectName) > 0 ) {
            projectDirectory = fileName.substring( 0, fileName.indexOf( projectName ) - 1 );
        }
        return projectDirectory;
    }
    
    private FileObject getFile() {
        if (lastDocument == null)
            return null;
        Source source = Source.create(lastDocument);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return fileObject;
    }

    private org.netbeans.api.project.Project getProject() {
        if (lastDocument == null)
            return null;
        Source source = Source.create(lastDocument);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return FileOwnerQuery.getOwner(fileObject);
    }
}
