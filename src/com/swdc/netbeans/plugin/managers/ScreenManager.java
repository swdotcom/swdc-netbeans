/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.openide.windows.WindowManager;
import java.util.logging.Logger;

/**
 *
 * @author xavierluiz
 */
public class ScreenManager {
    
    public static final Logger log = Logger.getLogger("ScreenManager");

    private static Frame ideFrame = null;
    private static double fullScreenHeight = 0;
    private static double fullScreenWidth = 0;
    protected static boolean checkingToDisableFlow = false;

    private static Frame getIdeWindow() {
        if (ideFrame == null) {
            ideFrame = WindowManager.getDefault().getMainWindow();
            if (ideFrame != null) {
                ideFrame.addWindowStateListener(new WindowStateListener() {
                    @Override
                    public void windowStateChanged(WindowEvent e) {
                        SwingUtilities.invokeLater(() -> {
                            FlowManager.checkToDisableFlow();
                        });
                    }
                });
            }
        }
        
        return ideFrame;
    }
    
    public static boolean isFullScreen() {
        Frame win = getIdeWindow();

        if (win != null) {

            // maximized both is actually maximized screen, which we
            // consider full screen as well
            if (win.getExtendedState() == JFrame.MAXIMIZED_BOTH || win.getState() == JFrame.MAXIMIZED_BOTH) {
                fullScreenHeight = win.getBounds().getHeight();
                fullScreenWidth = win.getBounds().getWidth();
                return true;
            } else if (win.getX() > 0) {
                return false;
            }

            // it may be full screen
            if (win.getBounds().getHeight() >= fullScreenHeight && win.getBounds().getWidth() >= fullScreenWidth) {
                return true;
            }
        }
        return false;
    }

    public static boolean enterFullScreen() {
        Frame win = getIdeWindow();
        if (win == null) {
            return false;
        }
        if (!isFullScreen()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    win.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    win.setBounds(win.getGraphicsConfiguration().getBounds());
                    win.setVisible(true);
                } catch (Exception e) {}

                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeTreeTopComponent.refresh();}, 1);
            });
            return true;
        }
        return false;
    }

    public static boolean exitFullScreen() {
        Frame win = getIdeWindow();
        if (win == null) {
            return false;
        }
        if (isFullScreen()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    win.setExtendedState(JFrame.NORMAL);
                    win.setBounds(win.getGraphicsConfiguration().getBounds());
                    win.setVisible(true);
                } catch (Exception e) {}
                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> {CodeTimeTreeTopComponent.refresh();}, 1);
            });
            return true;
        }
        return false;
    }
    
}
