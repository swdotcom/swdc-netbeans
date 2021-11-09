/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.status;

import com.software.codetime.metricstree.CodeTimeTreeTopComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;
import swdc.java.ops.manager.FileUtilManager;

/**
 *
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 1)
public class SoftwareStatusBar implements StatusLineElementProvider {

    private static JLabel statusLabel = new JLabel(" Code Time ");
    private JPanel panel = new JPanel(new BorderLayout());
    private long last_click_time = -1;
    
    private static StatusBarType lastStatusType = StatusBarType.PAW;
    private static String lastMsg = "";
    private static String lastTooltip = "";

    private static boolean registeredMouseClick = false;
    
    public static enum StatusBarType {
        PAW("com/software/codetime/assets/paw-grey.png"),
        ROCKET("com/software/codetime/assets/rocket.png"),
        OFF("com/software/codetime/assets/status-clock.png");

        private Icon icon;

        private StatusBarType(String iconStr) {
            icon = new ImageIcon(ImageUtilities.loadImage(iconStr));
        }
    }

    public SoftwareStatusBar() {
        statusLabel.setIcon(StatusBarType.PAW.icon);
        if (!registeredMouseClick) {
            registeredMouseClick = true;
            statusLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    CodeTimeTreeTopComponent.openTree();
                }
            });
        }
        panel.add(new JSeparator(SwingConstants.VERTICAL), BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.CENTER);
    }

    public void updateMessage(StatusBarType status, String text, String tooltip) {
        String name = FileUtilManager.getItem("name");
        
        tooltip = tooltip == null ? "" : tooltip;
        if (name != null && !name.equals("")) {
            tooltip += " (" + name + ")";
        }

        text = text == null || status.equals(StatusBarType.OFF) ? "" : text + " ";
        
        statusLabel.setText(text);
        switch (status) {
            case ROCKET:
                statusLabel.setIcon(StatusBarType.ROCKET.icon);
                break;
            case OFF:
                statusLabel.setIcon(StatusBarType.OFF.icon);
                break;
            default:
                statusLabel.setIcon(StatusBarType.PAW.icon);
                break;
        }
        statusLabel.repaint();
    }

    @Override
    public Component getStatusLineElement() {
        return panel;
    }
}
