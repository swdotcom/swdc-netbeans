/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.metricstree;

import java.awt.Component;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class IconTreeCellRenderer extends DefaultTreeCellRenderer {
    
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, true);
        
        this.setBorderSelectionColor(null);

        Icon icon = null;
        if (value instanceof MetricTreeNode) {
            MetricTreeNode node = (MetricTreeNode)value;
            
            String iconName = node.getIconName();
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

        return comp;
    }
    
    public ImageIcon createImageIcon(String iconName, String description) {
        URL imgURL = getClass().getResource("/com/swdc/netbeans/assets/" + iconName);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: /com/swdc/netbeans/assets/" + iconName);
            return null;
        }
    }
}
