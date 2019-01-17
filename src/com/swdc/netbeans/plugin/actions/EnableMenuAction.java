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
        id = "com.swdc.netbeans.plugin.actions.EnableMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_EnableMenuAction"
)
@ActionReference(path = "Menu/Tools/Software.com", position = -1)
@Messages("CTL_EnableMenuAction=Enable metrics")
public final class EnableMenuAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        SoftwareUtil.getInstance().updateTelementry(true);
    }
}
