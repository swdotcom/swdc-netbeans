/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.snowplow.tracker.events.UIInteractionType;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;
import org.openide.util.ImageUtilities;

/**
 *
 * @author xavierluiz
 */
public class AuthPromptManager {

    public static void initiateSwitchAccountFlow() {
        initiateAuthFlow("Switch account", "Switch to a different account?");
    }

    public static void initiateSignupFlow() {
        initiateAuthFlow("Sign up", "Sign up using...");
    }

    public static void initiateLoginFlow() {
        initiateAuthFlow("Log in", "Log in using...");
    }

    private static void initiateAuthFlow(String title, String message) {
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        Icon icon = new ImageIcon(ImageUtilities.loadImage("com/swdc/netbeans/plugin/assets/paw-grey.png"));
        String input = (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                icon,
                options, // Array of choices
                options[0]); // Initial choice
        if (StringUtils.isNotBlank(input)) {
            SoftwareSessionManager.launchLogin(input.toLowerCase(), UIInteractionType.click, true);
        }

    }
}
