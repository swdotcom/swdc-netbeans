/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import java.util.Arrays;
import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.JsonManager;
import java.util.ArrayList;
import java.util.List;

// id  (long), name (string), value (string), status (string), upgraded (int),
// last_datum_timestamp (long), version (string), authId (string), access_token (string),
// refresh_token (string), scopes: (string[]), plugin_uuid (string),
// createdAt (string), updatedAt (string), userId (long), pluginId (int)
public class Integration {
    public long id;
    public String name;
    public String value;
    public String status;
    public String authId;
    public String access_token;
    public String refresh_token;
    public List<String> scopes = new ArrayList<>();
    public String plugin_uuid;
    public String team_domain;
    public String team_name;
    
    public void updateInfoWithResponse(JsonObject data) {
        if (data != null) {
            this.id = JsonManager.getLongVal(data, "id", 0);
            this.name = JsonManager.getStringVal(data, "name", "");
            this.value = JsonManager.getStringVal(data, "value", "");
            this.status = JsonManager.getStringVal(data, "status", "");
            this.authId = JsonManager.getStringVal(data, "authId", "");
            this.access_token = JsonManager.getStringVal(data, "access_token", "");
            this.refresh_token = JsonManager.getStringVal(data, "refresh_token", "");
            this.plugin_uuid = JsonManager.getStringVal(data, "plugin_uuid", "");
            
            scopes = JsonManager.getStringArrayVal(data, "scopes");
        }
    }
}