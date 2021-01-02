/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.openide.util.Exceptions;

public class OsaScriptManager {
    public static void toggleDarkMode() {
        try {
            FileManager.setBooleanItem("checked_sys_events", true);
            Runtime runtime = Runtime.getRuntime();
            String[] args = { "osascript", "-e",
                "tell application \"System Events\"",
                "-e",
                "tell appearance preferences",
                "-e",
                "set dark mode to not dark mode",
                "-e",
                "end tell",
                "-e",
                "end tell"};
            Process p = runtime.exec(args);

            CodeTimeTreeTopComponent.rebuildTree();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static boolean isDarkMode() {
        boolean darkMode = false;

        // first check to see if the user has "System Events" authorized
        boolean checked_sys_events = FileManager.getBooleanItem("checked_sys_events");

        try {
            if (checked_sys_events) {
                String[] args = { "osascript", "-e",
                    "try",
                    "-e",
                    "tell application \"System Events\"",
                    "-e",
                    "tell appearance preferences",
                    "-e",
                    "set t_info to dark mode",
                    "-e",
                    "end tell",
                    "-e",
                    "end tell",
                    "-e",
                    "on error",
                    "-e",
                    "return false",
                    "-e",
                    "end try"};
                String result = SoftwareUtil.runCommand(args);
                if (StringUtils.isNotBlank(result)) {
                    try {
                        darkMode = Boolean.parseBoolean(result);
                    } catch (Exception e) {}
                }
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return darkMode;
    }

    // hide and unhide the dock
    public static void toggleDock() {
        try {
            FileManager.setBooleanItem("checked_sys_events", true);
            Runtime runtime = Runtime.getRuntime();
            String[] args = { "osascript", "-e",
                "tell application \"System Events\"",
                "-e",
                "tell dock preferences",
                "-e",
                "set x to autohide",
                "-e",
                "if x is false then",
                "-e",
                "set properties to {autohide:true}",
                "-e",
                "else",
                "-e",
                "set properties to {autohide:false}",
                "-e",
                "end if",
                "-e",
                "end tell",
                "-e",
                "end tell"};
            Process p = runtime.exec(args);

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
