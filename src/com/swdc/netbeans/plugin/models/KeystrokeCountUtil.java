/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;


import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileAggregateDataManager;
import com.swdc.netbeans.plugin.managers.SessionDataManager;
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

                // send the event to the event tracker
                EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeCountInfo);

                UtilManager.TimesData timesData = UtilManager.getTimesData();
                // set the latest payload timestamp utc so help with session time calculations
                FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error processing payload event: {0}", e.getMessage());
        }

        keystrokeCountInfo.resetData();
    }

    // end unended file payloads and add the cumulative editor seconds
    public static void preProcessKeystrokeData(CodeTime keystrokeCountInfo, long sessionSeconds, long elapsedSeconds) {

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

        // update the file info map
        FileAggregateDataManager.updateFileChangeInfo(fileChangeInfoMap);
    }
}
