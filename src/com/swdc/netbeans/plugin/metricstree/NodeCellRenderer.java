/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class NodeCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof JLabel) {
            this.setText(((JLabel)value).getText());
            this.setIcon(((JLabel)value).getIcon());
            this.setBorder(new EmptyBorder(2, 10, 2, 0));
        }
        return this;
    }
}
