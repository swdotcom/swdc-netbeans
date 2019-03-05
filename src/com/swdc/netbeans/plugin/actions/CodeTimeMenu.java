/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.actions;

import com.swdc.netbeans.plugin.SoftwareUtil;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.actions.Presenter;


@ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeMenu")
@ActionRegistration(displayName = "CodeTimeMenu", lazy = false)
@ActionReference(path = "Menu/Tools/Code Time", position = 1600)
public class CodeTimeMenu extends AbstractAction implements DynamicMenuContent, Presenter.Popup {

    @Override
    public JMenuItem getPopupPresenter() {
        JMenu menu = new JMenu(this);
        JComponent[] menuItems = createMenu();
        for (JComponent item : menuItems) {
            menu.add(item);
        }
        return menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //
    }

    @Override
    public JComponent[] getMenuPresenters() {
        return createMenu();
    }

    @Override
    public JComponent[] synchMenuPresenters(JComponent[] items) {
        return createMenu();
    }

    private JComponent[] createMenu() {
        SoftwareUtil.UserStatus userStatus = SoftwareUtil.getInstance().getUserStatus();
        List<JComponent> items = new ArrayList<>();

        items.add(toMenuItem(new CodeTimeDashboardAction()));
        items.add(toMenuItem(new WebDashboardAction()));
        items.add(toMenuItem(new CodeTimeTop40Action()));
        if (userStatus.loggedInUser == null) {
            // not logged in, show the login and signup menu items
            items.add(toMenuItem(new CodeTimeLoginAction()));
            items.add(toMenuItem(new CodeTimeSignupAction()));
        } else {
            items.add(toMenuItem(new CodeTimeLogoutAction()));
        }
        return items.toArray(new JComponent[items.size()]);
    }

    /**
     * Creates a menu item from an action.
     *
     * @param action an action
     * @return JMenuItem
     */
    private static JMenuItem toMenuItem(Action action) {
        JMenuItem item;
        if (action instanceof Presenter.Menu) {
            item = ((Presenter.Menu) action).getMenuPresenter();
        } else if (action instanceof Presenter.Popup) {
            item = ((Presenter.Popup) action).getPopupPresenter();
        } else {
            item = new JMenuItem();
            item.addActionListener(action);
            Actions.connect(item, action, false);
        }
        return item;
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.WebDashboardAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class WebDashboardAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().launchWebDashboard();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Web dashboard");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeDashboardAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeDashboardAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().launchCodeTimeMetricsDashboard();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Code time dashboard");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeLoginAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeLoginAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().launchLogin();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Log in to see your coding data");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeLogoutAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeLogoutAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().pluginLogout();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            SoftwareUtil.UserStatus userStatus = SoftwareUtil.getInstance().getUserStatus();
            String emailPart = (userStatus != null && userStatus.loggedInUser != null)
                    ? " (" + userStatus.email + ")" : "";
            JMenuItem item = new JMenuItem("Log out of Code Time" + emailPart);
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeSignupAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeSignupAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().launchSignup();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Sign up a new account");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeTop40Action")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeTop40Action extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.getInstance().launchSoftwareTopForty();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Software top 40");
            item.addActionListener(this);
            return item;
        }
    }
    

}