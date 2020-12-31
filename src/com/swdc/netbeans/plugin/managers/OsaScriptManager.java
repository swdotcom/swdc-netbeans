/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import org.apache.commons.lang.StringUtils;

public class OsaScriptManager {
    public static void toggleDarkMode() {
        FileManager.setBooleanItem("checked_sys_events", true);
        String darkmodeScript = "tell application \"System Events\"\n" +
                "tell appearance preferences\n" +
                "set dark mode to not dark mode\n" +
                "end tell\n" +
                "end tell";
        String[] args = {"osascript", "-e", darkmodeScript};
        SoftwareUtil.runCommand(args);
        
        CodeTimeTreeTopComponent.rebuildTree();
    }

    public static boolean isDarkMode() {
        boolean darkMode = false;

        // first check to see if the user has "System Events" authorized
        boolean checked_sys_events = FileManager.getBooleanItem("checked_sys_events");

        if (checked_sys_events) {
            String darkModeFlagScript = "try\n" +
                    "tell application \"System Events\"\n" +
                    "tell appearance preferences\n" +
                    "set t_info to dark mode\n" +
                    "end tell\n" +
                    "end tell\n" +
                    "on error\n" +
                    "return false\n" +
                    "end try";
            String[] args = {"osascript", "-e", darkModeFlagScript};
            String result = SoftwareUtil.runCommand(args);
            if (StringUtils.isNotBlank(result)) {
                try {
                    darkMode = Boolean.parseBoolean(result);
                } catch (Exception e) {}
            }
        }
        return darkMode;
    }

    // hide and unhide the dock
    public static void toggleDock() {
        FileManager.setBooleanItem("checked_sys_events", true);
        String toggleDockScript = "tell application \"System Events\"\n" +
                "tell dock preferences\n" +
                "set x to autohide\n" +
                "if x is false then\n" +
                "set properties to {autohide:true}\n" +
                "else\n" +
                "set properties to {autohide:false}\n" +
                "end if\n" +
                "end tell\n" +
                "end tell";
        String[] args = {"osascript", "-e", toggleDockScript};
        SoftwareUtil.runCommand(args);
    }
}
