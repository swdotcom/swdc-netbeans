/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import com.google.gson.JsonObject;
import com.swdc.netbeans.plugin.managers.JsonManager;

public class SlackUserProfile {
    public String avatar_hash;
    public String status_text;
    public String status_emoji;
    public long status_expiration;
    public String real_name;
    public String display_name;
    public String real_name_normalized;
    public String email;
    public String image_original;
    public String image_24;
    public String image_32;
    public String image_48;
    public String image_72;
    public String image_192;
    public String image_512;
    public String team;
    
    public void updateInfoWithResponse(JsonObject data) {
        // {ok (bool), profile: {avatar_hash, status_text, status_emoji, status_expiration, real_name,
        //  display_name, real_name_normalized, display_name_normalized, email, image_original, image_..., team}}
        if (data != null && !data.get("profile").isJsonNull()) {
            JsonObject profile = data.get("profile").getAsJsonObject();
            this.avatar_hash = JsonManager.getStringVal(profile, "avatar_hash", "");
            this.status_text = JsonManager.getStringVal(profile, "status_text", "");
            this.status_emoji = JsonManager.getStringVal(profile, "status_emoji", "");
            this.status_expiration = JsonManager.getLongVal(profile, "status_expiration", 0);
            this.real_name = JsonManager.getStringVal(profile, "real_name", "");
            this.display_name = JsonManager.getStringVal(profile, "display_name", "");
            this.real_name_normalized = JsonManager.getStringVal(profile, "real_name_normalized", "");
            this.email = JsonManager.getStringVal(profile, "email", "");
            this.image_original = JsonManager.getStringVal(profile, "image_original", "");
            this.team = JsonManager.getStringVal(profile, "team", "");
        }
    }
}
