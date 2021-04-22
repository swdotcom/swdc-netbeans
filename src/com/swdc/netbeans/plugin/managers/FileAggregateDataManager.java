/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.swdc.netbeans.plugin.SoftwareUtil;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.model.FileChangeInfo;

public class FileAggregateDataManager {

    public static void clearFileChangeInfoSummaryData() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }

    public static Map<String, FileChangeInfo> getFileChangeInfo() {
        Map<String, FileChangeInfo> fileInfoMap = new HashMap<>();
        JsonObject jsonObj = FileUtilManager.getFileContentAsJson(FileUtilManager.getFileChangeSummaryFile());
        if (jsonObj != null) {
            Type type = new TypeToken<Map<String, FileChangeInfo>>() {}.getType();
            fileInfoMap = SoftwareUtil.gson.fromJson(jsonObj, type);
        } else {
            // create it
            FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
        }
        return fileInfoMap;
    }

    public static void updateFileChangeInfo(Map<String, FileChangeInfo> fileInfoMap) {
        FileUtilManager.writeData(FileUtilManager.getFileChangeSummaryFile(), fileInfoMap);
    }
}
