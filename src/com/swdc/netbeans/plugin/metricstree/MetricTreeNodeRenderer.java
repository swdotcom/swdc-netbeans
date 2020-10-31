/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.net.URL;

public class MetricTreeNodeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, true);

        
        Icon icon = null;
        if (value instanceof MetricTreeNode) {
            String iconName = ((MetricTreeNode)value).getIconName();
            if (iconName != null) {
                ImageIcon imageIcon = createImageIcon(iconName, "");
                if (imageIcon != null) {
                    icon = imageIcon;
                }
            }
        }

        if (icon != null) {
            setIcon(icon);
        }

        return component;
    }
    
    public ImageIcon createImageIcon(String iconName, String description) {
        URL imgURL = getClass().getResource("/com/swdc/netbeans/plugin/assets/" + iconName);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: /com/softwareco/intellij/plugin/assets/" + iconName);
            return null;
        }
    }

}