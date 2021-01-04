/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.snowplow.tracker.events.UIInteractionType;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author xavierluiz
 */
public class SwitchAccountManager {

    public static void initiateSwitchAccountFlow() {
        String[] options = new String[]{"Google", "GitHub", "Email"};
        String input = (String) JOptionPane.showInputDialog(
                null,
                "Switch to a different account?",
                "Switch account",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options, // Array of choices
                options[0]); // Initial choice
        if (StringUtils.isNotBlank(input)) {
            SoftwareSessionManager.launchLogin(input.toLowerCase(), UIInteractionType.click, true);
        }
    }

    public static void initiateSignupFlow() {
        // @DialogMessage String message, @DialogTitle String title, @Nullable Icon icon,
        // String[] values, String initialValue, @Nullable InputValidator validator
        String[] options = new String[]{"Google", "GitHub", "Email"};
        String input = (String) JOptionPane.showInputDialog(
                null,
                "Sign up using..",
                "Sign up",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options, // Array of choices
                options[0]); // Initial choice
        if (StringUtils.isNotBlank(input)) {
            SoftwareSessionManager.launchLogin(input.toLowerCase(), UIInteractionType.click, true);
        }
    }
}
