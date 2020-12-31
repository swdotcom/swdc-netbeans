/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.managers.JsonManager;


public class SlackUserPresence {
    public boolean auto_away = true;
    public int connection_count;
    public long last_activity;
    public boolean manual_away = false;
    public boolean ok = true;
    public boolean online = true;
    public String presence = "active"; // ['active'|'away']
    
    public void updateInfoWithResponse(JsonObject data) {
        // {ok (bool), presence (string), online (bool), auto_away (bool), manual_away (bool),
        //  connection_count (int), last_activity (long)}
        if (data != null) {
            this.auto_away = JsonManager.getBooleanVal(data, "auto_away", false);
            this.connection_count = JsonManager.getIntVal(data, "connection_count", 0);
            this.last_activity = JsonManager.getLongVal(data, "last_activity", 0);
            this.manual_away = JsonManager.getBooleanVal(data, "manual_away", false);
            this.ok = JsonManager.getBooleanVal(data, "ok", false);
            this.online = JsonManager.getBooleanVal(data, "online", false);
            this.presence = JsonManager.getStringVal(data, "presence", "active");
        }
    }
}
