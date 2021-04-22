/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;


import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileAggregateDataManager;
import com.swdc.netbeans.plugin.managers.SessionDataManager;
import com.swdc.netbeans.plugin.managers.TimeDataManager;
import com.swdc.netbeans.plugin.managers.WallClockManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.project.Project;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.CodeTime;
import swdc.java.ops.model.ElapsedTime;
import swdc.java.ops.model.FileChangeInfo;
import swdc.java.ops.model.TimeData;

public class KeystrokeCountUtil {
    
    public static final Logger LOG = Logger.getLogger("KeystrokeCount");

    private String type = "Events";

    public long cumulative_editor_seconds = 0;
    public long elapsed_seconds = 0;
    public String project_null_error = "";


    public static void processKeystrokes(CodeTime keystrokeCountInfo) {
        try {
            if (keystrokeCountInfo.hasData()) {

                swdc.java.ops.model.Project project = keystrokeCountInfo.getProject();

                // check to see if we need to find the main project if we don't have it
                if (project == null || StringUtils.isBlank(project.getDirectory()) ||
                        project.getDirectory().equals("Untitled")) {
                    
                    Project p = SoftwareUtil.getFirstActiveProject();
                    if (p != null) {
                        project.setDirectory(p.getProjectDirectory().getPath());
                        project.setName(p.getProjectDirectory().getName());
                    } else {
                        project = new swdc.java.ops.model.Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
                    }
                }

                ElapsedTime eTime = SessionDataManager.getTimeBetweenLastPayload();

                // end the file end times.
                preProcessKeystrokeData(keystrokeCountInfo, eTime.sessionSeconds, eTime.elapsedSeconds);

                // update the file aggregate info
                updateAggregates(keystrokeCountInfo, eTime.sessionSeconds);

                // send the event to the event tracker
                EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeCountInfo);

                // refresh the code time tree view
                WallClockManager.getInstance().dispatchStatusViewUpdate(false);

                UtilManager.TimesData timesData = UtilManager.getTimesData();
                // set the latest payload timestamp utc so help with session time calculations
                FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error processing payload event: {0}", e.getMessage());
        }

        keystrokeCountInfo.resetData();
    }

    private static void validateAndUpdateCumulativeData(CodeTime keystrokeCountInfo, long sessionSeconds) {

        TimeData td = TimeDataManager.incrementSessionAndFileSeconds(keystrokeCountInfo.getProject(), sessionSeconds);

        // get the current payloads so we can compare our last cumulative seconds
        if (SoftwareUtil.isNewDay()) {

            // clear out data from the previous day
            WallClockManager.getInstance().newDayChecker();

            if (td != null) {
                td = null;
                keystrokeCountInfo.project_null_error = "TimeData should be null as its a new day";
            }
        }

        // add the cumulative data
        keystrokeCountInfo.workspace_name = SoftwareUtil.getWorkspaceName();
        keystrokeCountInfo.hostname = UtilManager.getHostname();
        keystrokeCountInfo.cumulative_session_seconds = 60;
        keystrokeCountInfo.cumulative_editor_seconds = 60;

        if (td != null) {
            keystrokeCountInfo.cumulative_editor_seconds = td.getEditor_seconds();
            keystrokeCountInfo.cumulative_session_seconds = td.getSession_seconds();
        }

        if (keystrokeCountInfo.cumulative_editor_seconds < keystrokeCountInfo.cumulative_session_seconds) {
            keystrokeCountInfo.cumulative_editor_seconds = keystrokeCountInfo.cumulative_session_seconds;
        }
    }

    // end unended file payloads and add the cumulative editor seconds
    public static void preProcessKeystrokeData(CodeTime keystrokeCountInfo, long sessionSeconds, long elapsedSeconds) {

        validateAndUpdateCumulativeData(keystrokeCountInfo, sessionSeconds);

        // set the elapsed seconds (last end time to this end time)
        keystrokeCountInfo.elapsed_seconds = elapsedSeconds;

        UtilManager.TimesData timesData = UtilManager.getTimesData();
        Map<String, CodeTime.FileInfo> fileInfoDataSet = keystrokeCountInfo.getSource();
        for ( CodeTime.FileInfo fileInfoData : fileInfoDataSet.values() ) {
            // end the ones that don't have an end time
            if (fileInfoData.end == 0) {
                // set the end time for this file
                fileInfoData.end = timesData.now;
                fileInfoData.local_end = timesData.local_now;
            }
        }
    }

    private static void updateAggregates(CodeTime keystrokeCountInfo, long sessionSeconds) {
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        KeystrokeAggregate aggregate = new KeystrokeAggregate();
        if (keystrokeCountInfo.getProject() != null) {
            aggregate.directory = keystrokeCountInfo.getProject().getDirectory();
        } else {
            aggregate.directory = "Untitled";
        }
        for (String key : keystrokeCountInfo.getSource().keySet()) {
            CodeTime.FileInfo fileInfo = keystrokeCountInfo.getSource().get(key);
            fileInfo.duration_seconds = fileInfo.end - fileInfo.start;
            fileInfo.fsPath = key;
            try {
                Path path = Paths.get(key);
                if (path != null) {
                    Path fileName = path.getFileName();
                    if (fileName != null) {
                        fileInfo.name = fileName.toString();
                    }
                }

                aggregate.aggregate(fileInfo);

                FileChangeInfo existingFileInfo = fileChangeInfoMap.get(key);
                if (existingFileInfo == null) {
                    existingFileInfo = new FileChangeInfo();
                    fileChangeInfoMap.put(key, existingFileInfo);
                }
                existingFileInfo.aggregate(fileInfo);
                existingFileInfo.kpm = existingFileInfo.keystrokes / existingFileInfo.update_count;
            } catch (Exception e) {
                // error getting the path
            }
        }

        // update the aggregate info
        SessionDataManager.incrementSessionSummary(aggregate, sessionSeconds);

        // update the file info map
        FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);
    }
}
