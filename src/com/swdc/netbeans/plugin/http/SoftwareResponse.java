/**
 * Copyright (c) 2019 by Software.com
 * All rights reserved
 */
package com.swdc.netbeans.plugin.http;

import com.google.gson.JsonObject;

/**
 *
 */
public class SoftwareResponse {
    private boolean ok = false;
    private int code;
    private String errorMessage;
    private String jsonStr;
    private JsonObject jsonObj;

    public boolean isOk() {
        return ok;
    }

    public void setIsOk(boolean ok) {
        this.ok = ok;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getJsonStr() {
        return jsonStr;
    }

    public void setJsonStr(String jsonStr) {
        this.jsonStr = jsonStr;
    }

    public JsonObject getJsonObj() {
        return jsonObj;
    }

    public void setJsonObj(JsonObject jsonObj) {
        this.jsonObj = jsonObj;
    }
    
}
