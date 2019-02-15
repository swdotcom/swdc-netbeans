/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.actions;

import com.swdc.netbeans.plugin.SoftwareUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

@ActionID(
        category = "Tools",
        id = "com.swdc.netbeans.plugin.actions.SoftwareTopFortyAction"
)
@ActionRegistration(
        displayName = "#CTL_SoftwareTopFortyAction"
)
@ActionReference(path = "Menu/Tools/Code Time", position = 2)
@NbBundle.Messages("CTL_SoftwareTopFortyAction=Software top 40")
public final class SoftwareTopFortyAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        SoftwareUtil.getInstance().launchSoftwareTopForty();
    }
}
