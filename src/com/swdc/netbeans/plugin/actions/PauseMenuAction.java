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
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Tools",
        id = "com.swdc.netbeans.plugin.actions.PauseMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_PauseMenuAction"
)
@ActionReference(path = "Menu/Tools/Software.com", position = -2)
@Messages("CTL_PauseMenuAction=Pause metrics")
public final class PauseMenuAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        SoftwareUtil.getInstance().updateTelementry(false);
    }
}
