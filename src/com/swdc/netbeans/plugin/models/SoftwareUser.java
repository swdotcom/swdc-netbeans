/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import java.util.ArrayList;
import java.util.List;

public class SoftwareUser {
    public int registered = 0;
    public String email = "";
    public List<Integration> integrations = new ArrayList<>();
    public String plugin_jwt = "";
}
