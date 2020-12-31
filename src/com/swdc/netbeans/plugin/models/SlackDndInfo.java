/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.managers.JsonManager;


public class SlackDndInfo {
    public boolean dnd_enabled;
    public int next_dnd_end_ts;
    public int next_dnd_start_ts;
    public int snooze_endtime;
    public int snooze_remaining;
    public boolean ok;
    public boolean snooze_enabled;
    
    public void updateInfoWithResponse(JsonObject data) {
        // {ok (bool), dnd_enabled (bool), next_dnd_start_ts (int), next_dnd_end_ts (int),
        //  snooze_enabled (bool), snooze_endtime (int), snooze_remaining (int)
        if (data != null) {
            this.dnd_enabled = JsonManager.getBooleanVal(data, "dnd_enabled", false);
            this.next_dnd_end_ts = JsonManager.getIntVal(data, "next_dnd_end_ts", 0);
            this.next_dnd_start_ts = JsonManager.getIntVal(data, "next_dnd_start_ts", 0);
            this.ok = JsonManager.getBooleanVal(data, "ok", true);
            this.snooze_enabled = JsonManager.getBooleanVal(data, "snooze_enabled", false);
            this.snooze_endtime = JsonManager.getIntVal(data, "snooze_endtime", 0);
            this.snooze_remaining = JsonManager.getIntVal(data, "snooze_remaining", 0);
        }
    }
}
