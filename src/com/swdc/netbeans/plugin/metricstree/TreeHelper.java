/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author xavierluiz
 */
public class TreeHelper {
    
    public static final String GOOGLE_SIGNUP_ID = "google";
    public static final String GITHIUB_SIGNUP_ID = "github";
    public static final String EMAIL_SIGNUP_ID = "email";
    public static final String LOGGED_IN_ID = "logged-in";
    
    public static List<MetricTreeNode> buildSignupNodes() {
        List<MetricTreeNode> list = new ArrayList<MetricTreeNode>();
        String name = FileManager.getItem("name");
        if (name == null || name.equals("")) {
            list.add(buildSignupNode("google"));
            list.add(buildSignupNode("github"));
            list.add(buildSignupNode("email"));
        } else {
            list.add(buildLoggedInNode());
        }
        return list;
    }
    
    private static MetricTreeNode buildSignupNode(String type) {
        String iconName = "envelope.svg";
        String text = "Sign up with email";
        String id = EMAIL_SIGNUP_ID;
        if (type.equals("google")) {
            iconName = "icons8-google.svg";
            text = "Sign up with Google";
            id = GOOGLE_SIGNUP_ID;
        } else if (type.equals("github")) {
            iconName = "icons8-github.svg";
            text = "Sign up with GitHub";
            id = GITHIUB_SIGNUP_ID;
        }
        MetricTreeNode node = new MetricTreeNode(text, iconName, id);
        return node;
    }
    
    private static MetricTreeNode buildLoggedInNode() {
        String authType = FileManager.getItem("authType");
        String name = FileManager.getItem("name");
        String iconName = "envelope.svg";
        if ("google".equals(authType)) {
            iconName = "icons8-google.svg";
        } else if ("github".equals(authType)) {
            iconName = "icons8-github.svg";
        }
        
        MetricTreeNode node = new MetricTreeNode(name, iconName, LOGGED_IN_ID);
        return node;
    }
    
    public static void handleClickEvent(MetricTreeNode node) {
        switch (node.getId()) {
            case GOOGLE_SIGNUP_ID:
            case GITHIUB_SIGNUP_ID:
            case EMAIL_SIGNUP_ID:
                break;
            case LOGGED_IN_ID:
                break;
            default:
                break;
        }
    }
    
}
