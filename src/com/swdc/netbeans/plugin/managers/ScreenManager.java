/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import java.awt.Frame;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.openide.windows.WindowManager;

/**
 *
 * @author xavierluiz
 */
public class ScreenManager {
    
    public static boolean isFullScreen() {
        try {
            Frame frame = WindowManager.getDefault().getMainWindow();
            if (frame != null) {
                return (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH);
            }
        } catch (Exception e) {
            //
        }
        return false;
    }
    
    public static void toggleFullScreenMode() {
        try {
            SwingUtilities.invokeLater(() -> {
                Frame frame = WindowManager.getDefault().getMainWindow();
                if (frame != null) {
                    int extState = frame.getExtendedState();
                    if (extState == JFrame.MAXIMIZED_BOTH) {
                        frame.setExtendedState(JFrame.NORMAL);
                    } else {
                        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    SwingUtilities.invokeLater(() -> {
                        CodeTimeTreeTopComponent.refresh();
                    });
                }
            });
        } catch (Exception e) {
            //
        }
    }
    
}
