/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.software.codetime.SoftwareUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JsonManager {
    public static int getIntVal(JsonObject obj, String attribute, int defaultVal) {
        if (obj != null && obj.has(attribute) && !obj.get(attribute).isJsonNull()) {
            return obj.get(attribute).getAsInt();
        }
        return defaultVal;
    }
    
    public static String getStringVal(JsonObject obj, String attribute, String defaultVal) {
        if (obj != null && obj.has(attribute) && !obj.get(attribute).isJsonNull()) {
            return obj.get(attribute).getAsString();
        }
        return defaultVal;
    }
    
    public static boolean getBooleanVal(JsonObject obj, String attribute, boolean defaultVal) {
        if (obj != null && obj.has(attribute) && !obj.get(attribute).isJsonNull()) {
            return obj.get(attribute).getAsBoolean();
        }
        return defaultVal;
    }
    
    public static JsonArray getArrayVal(JsonObject obj, String attribute) {
        return (obj.has(attribute) && !obj.get(attribute).isJsonNull())
                ? obj.get(attribute).getAsJsonArray() : new JsonArray();
    }
    
    public static List<String> getStringArrayVal(JsonObject obj, String attribute) {
        if (obj.has(attribute) && !obj.get(attribute).isJsonNull()) {
            try {
                String scopesStrArray = obj.get(attribute).getAsString();
                String[] scopesArray = SoftwareUtil.gson.fromJson(scopesStrArray, String[].class);
                return Arrays.asList(scopesArray);
            } catch (Exception e) {
                //
            }
        }
        return new ArrayList<>();
    }
    
    public static long getLongVal(JsonObject obj, String attribute, long defaultVal) {
        if (obj != null && obj.has(attribute) && !obj.get(attribute).isJsonNull()) {
            return obj.get(attribute).getAsLong();
        }
        return defaultVal;
    }
    
    public static JsonObject getJsonObjectVal(JsonObject obj, String attribute) {
        return obj != null && !obj.get(attribute).isJsonNull() ? obj.get(attribute).getAsJsonObject() : null;
    }
}
