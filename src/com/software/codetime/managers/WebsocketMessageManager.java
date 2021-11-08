/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.managers;

import com.google.gson.JsonObject;
import swdc.java.ops.model.SessionSummary;
import swdc.java.ops.websockets.WebsocketMessageHandler;

/**
 *
 * @author xavierluiz
 */
public class WebsocketMessageManager implements WebsocketMessageHandler {
    @Override
    public void handleFlowScore(JsonObject jsonObject) {
        FlowManager.enterFlowMode(true);
    }

    @Override
    public void handleFlowState(boolean enable_flow) {
        if (enable_flow) {
            FlowManager.enterFlowMode(true);
        } else {
            FlowManager.exitFlowMode();
        }
    }

    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        SessionDataManager.updateFileSummaryAndStatsBar(sessionSummary);
    }
}
