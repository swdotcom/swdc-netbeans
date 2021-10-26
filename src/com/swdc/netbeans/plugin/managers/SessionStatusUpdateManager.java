/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import javax.swing.SwingUtilities;
import swdc.java.ops.manager.StatusBarUpdateHandler;
import swdc.java.ops.model.SessionSummary;

public class SessionStatusUpdateManager implements StatusBarUpdateHandler {
    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        SwingUtilities.invokeLater(() -> {
        	SessionDataManager.updateFileSummaryAndStatsBar(sessionSummary);
        });
    }
}

