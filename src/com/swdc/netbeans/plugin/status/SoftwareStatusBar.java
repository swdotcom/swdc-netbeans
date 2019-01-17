/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.status;

import com.swdc.netbeans.plugin.SoftwareUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 */
@ServiceProvider(service = StatusLineElementProvider.class, position = 1)
public class SoftwareStatusBar implements StatusLineElementProvider {

    private static JLabel statusLabel = new JLabel(" Software.com ");
    private JPanel panel = new JPanel(new BorderLayout());

    public enum StatusBarType {
        ROCKET("com/swdc/netbeans/plugin/status/rocket.png"),
        FULL("com/swdc/netbeans/plugin/status/100.png"),
        ALMOST("com/swdc/netbeans/plugin/status/75.png"),
        HALF("com/swdc/netbeans/plugin/status/50.png"),
        QUARTER("com/swdc/netbeans/plugin/status/25.png"),
        NO_KPM("com/swdc/netbeans/plugin/status/sw.png"),
        ALERT("com/swdc/netbeans/plugin/status/warning.png");

        private Icon icon;

        private StatusBarType(String iconStr) {
            icon = new ImageIcon(ImageUtilities.loadImage(iconStr));
        }
    }

    public SoftwareStatusBar() {
        statusLabel.setIcon(StatusBarType.NO_KPM.icon);
        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SoftwareUtil.getInstance().launchDashboard();
            }
        });
        panel.add(new JSeparator(SwingConstants.VERTICAL), BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.CENTER);
    }

    public void updateMessage(StatusBarType status, String text, String tooltip) {
        statusLabel.setText(text + " ");
        statusLabel.setToolTipText(tooltip);
        switch (status) {
            case ROCKET:
                statusLabel.setIcon(StatusBarType.ROCKET.icon);
                break;
            case FULL:
                statusLabel.setIcon(StatusBarType.FULL.icon);
                break;
            case ALMOST:
                statusLabel.setIcon(StatusBarType.ALMOST.icon);
                break;
            case HALF:
                statusLabel.setIcon(StatusBarType.HALF.icon);
                break;
            case QUARTER:
                statusLabel.setIcon(StatusBarType.QUARTER.icon);
                break;
            default:
                statusLabel.setIcon(StatusBarType.NO_KPM.icon);
                break;
        }
    }

    @Override
    public Component getStatusLineElement() {
        return panel;
    }
}
