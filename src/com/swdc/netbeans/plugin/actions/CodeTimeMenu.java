/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.actions;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileManager;
import com.swdc.netbeans.plugin.managers.SoftwareSessionManager;
import com.swdc.netbeans.plugin.metricstree.CodeTimeTreeTopComponent;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.apache.commons.lang.StringUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Actions;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.actions.Presenter;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;


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
        List<JComponent> items = new ArrayList<>();

        items.add(toMenuItem(new CodeTimeDashboardAction()));
        items.add(toMenuItem(new WebDashboardAction()));

        String name = FileUtilManager.getItem("name");
        if (StringUtils.isEmpty(name)) {
            // not logged in, show the login and signup menu items
            items.add(toMenuItem(new CodeTimeSignupAction()));
        }
        if (SlackManager.hasSlackWorkspaces()) {
            items.add(toMenuItem(new DisconnectSlackAction()));
        }
        items.add(toMenuItem(new ConnectSlackAction()));
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
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.DisconnectSlackAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class DisconnectSlackAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SlackManager.disconnectSlackWorkspace(() -> {CodeTimeTreeTopComponent.refresh();});
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Disconnect Slack workspace");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.ConnectSlackAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class ConnectSlackAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SlackManager.connectSlackWorkspace(() -> {CodeTimeTreeTopComponent.refresh();});
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Connect Slack workspace");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.WebDashboardAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class WebDashboardAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareSessionManager.launchWebDashboard(UIInteractionType.keyboard);
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("More data at Software.com");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeDashboardAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeDashboardAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.launchCodeTimeMetricsDashboard();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Dashboard");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeSignupAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeSignupAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.showAuthSelectPrompt(true);
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Sign up to see your coding data");
            item.addActionListener(this);
            return item;
        }
    }

}