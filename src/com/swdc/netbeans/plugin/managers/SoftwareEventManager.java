/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;


import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.models.KeystrokeCount;
import com.swdc.netbeans.plugin.models.KeystrokeProject;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;

public class SoftwareEventManager {

    public static final Logger LOG = Logger.getLogger("SoftwareCoEventManager");

    private static SoftwareEventManager instance = null;

    private static final int FOCUS_STATE_INTERVAL_SECONDS = 5;
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");
    private static final Pattern NEW_LINE_TAB_PATTERN = Pattern.compile("\n\t");
    private static final Pattern TAB_PATTERN = Pattern.compile("\t");

    public static boolean isCurrentlyActive = true;

    private EventTrackerManager tracker;
    private KeystrokeManager keystrokeMgr;
    private SoftwareSessionManager sessionMgr;
    private AsyncManager asyncManager;
    private Document lastDocument;

    public static SoftwareEventManager getInstance() {
        if (instance == null) {
            instance = new SoftwareEventManager();
        }
        return instance;
    }

    private SoftwareEventManager() {
        keystrokeMgr = KeystrokeManager.getInstance();
        sessionMgr = SoftwareSessionManager.getInstance();
        tracker = EventTrackerManager.getInstance();
        asyncManager = AsyncManager.getInstance();

        final Runnable checkFocusStateTimer = () -> checkFocusState();
        asyncManager.scheduleService(
                checkFocusStateTimer, "checkFocusStateTimer", 0, FOCUS_STATE_INTERVAL_SECONDS);
    }

    private void checkFocusState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean isActive = true; // TODO: figure out how to get the focus status
                if (isActive != isCurrentlyActive) {
                    KeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount();
                    if (keystrokeCount != null) {
                        // set the flag the "unfocusStateChangeHandler" will look for in order to process payloads early
                        keystrokeCount.triggered = false;
                        keystrokeCount.processKeystrokes();
                    }
                    EventTrackerManager.getInstance().trackEditorAction("editor", "unfocus");
                } else {
                    // just set the process keystrokes payload to false since we're focused again
                    EventTrackerManager.getInstance().trackEditorAction("editor", "focus");
                }

                // update the currently active flag
                isCurrentlyActive = isActive;
            }
        });
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

    private KeystrokeCount getCurrentKeystrokeCount(Project proj) {
        KeystrokeCount keystrokeCount = keystrokeMgr.getKeystrokeCount();
        if (keystrokeCount == null) {
            // create one
            String projectName = proj != null ? proj.getProjectDirectory().getName() : SoftwareUtil.UNNAMED_PROJECT;
            String projectDir = proj != null  ? proj.getProjectDirectory().getPath() : SoftwareUtil.UNTITLED_FILE;
            // create the keysrtroke count wrapper
            createKeystrokeCountWrapper(projectName, projectDir);

            // now retrieve it from the mgr
            keystrokeCount = keystrokeMgr.getKeystrokeCount();
        }
        return keystrokeCount;
    }

    private void updateFileInfoMetrics(Document document, DocumentEvent e, KeystrokeCount.FileInfo fileInfo, KeystrokeCount keystrokeCount) {

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
        
        EventType etype = e.getType();
        int numKeystrokes = 0;
        int numDeleteKeystrokes = 0;
        if (etype.equals(EventType.INSERT)) {
            
        } else if (etype.equals(EventType.REMOVE)) {
            
        } else if (etype.equals(EventType.CHANGE)) {
            
        }
        
        String text = "";
        String oldText = "";
        
//        String text = documentEvent.getNewFragment() != null ? documentEvent.getNewFragment().toString() : "";
//        String oldText = documentEvent.getOldFragment() != null ? documentEvent.getOldFragment().toString() : "";
//
//        int new_line_count = document.getLineCount();
//        fileInfo.length = document.getTextLength();
//
//        // this will give us the positive char change length
//        int numKeystrokes = documentEvent.getNewLength();
//        // this will tell us delete chars
//        int numDeleteKeystrokes = documentEvent.getOldLength();

        // check if its an auto indent
        boolean hasAutoIndent = text.matches("^\\s{2,4}$") || TAB_PATTERN.matcher(text).find();
        boolean newLineAutoIndent = text.matches("^\n\\s{2,4}$") || NEW_LINE_TAB_PATTERN.matcher(text).find();

        // update the deletion keystrokes if there are lines removed
        numDeleteKeystrokes = numDeleteKeystrokes >= linesRemoved ? numDeleteKeystrokes - linesRemoved : numDeleteKeystrokes;

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
    public void handleSelectionChangedEvents(String fileName, Project project) {
        KeystrokeCount keystrokeCount = getCurrentKeystrokeCount(project);

        KeystrokeCount.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        keystrokeCount.endPreviousModifiedFiles(fileName);
    }

    public void handleFileOpenedEvents(String fileName, Project project) {
        
        KeystrokeCount keystrokeCount = getCurrentKeystrokeCount(project);

        KeystrokeCount.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
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

    public void handleFileClosedEvents(String fileName, Project project) {
        KeystrokeCount keystrokeCount = getCurrentKeystrokeCount(project);
        KeystrokeCount.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
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
        
        Project p = getProject();
        KeystrokeCount keystrokeCount = getCurrentKeystrokeCount(p);
        
        KeystrokeCount.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (StringUtils.isBlank(fileInfo.syntax)) {
            // get the grammar
            String fileType = fileObj.getName();
            if (fileType != null && !fileType.equals("")) {
                fileInfo.syntax = fileType;
            }
        }
        
        fileInfo.lines = SoftwareUtil.getLineCount(fileName);

    }

    public void createKeystrokeCountWrapper(String projectName, String projectFilepath) {
        //
        // Create one since it hasn't been created yet
        // and set the start time (in seconds)
        //
        KeystrokeCount keystrokeCount = new KeystrokeCount();

        KeystrokeProject keystrokeProject = new KeystrokeProject(projectName, projectFilepath );
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

    private Project getProject() {
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
