/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.actions;

import com.swdc.netbeans.plugin.SoftwareUtil;
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
import javax.swing.SwingUtilities;
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
        boolean loggedIn = SoftwareUtil.getUserLoginState();
        List<JComponent> items = new ArrayList<>();

        items.add(toMenuItem(new CodeTimeDashboardAction()));
        items.add(toMenuItem(new CodeTimeTop40Action()));
        items.add(toMenuItem(new WebDashboardAction()));

        if (!loggedIn) {
            // not logged in, show the login and signup menu items
            items.add(toMenuItem(new CodeTimeLoginAction()));
        }
        items.add(toMenuItem(new CodeTimeToggleStatusAction()));
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
            SoftwareSessionManager.launchWebDashboard(UIInteractionType.keyboard);
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
            SoftwareUtil.launchCodeTimeMetricsDashboard();
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
            SoftwareSessionManager.launchLogin("email", UIInteractionType.keyboard, false);
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Log in to see your coding data");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeTop40Action")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeTop40Action extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.launchSoftwareTopForty();
        }

        @Override
        public JMenuItem getMenuPresenter() {
            JMenuItem item = new JMenuItem("Software top 40");
            item.addActionListener(this);
            return item;
        }
    }
    
    @ActionID(category = "CodeTimeMenu", id = "com.swdc.netbeans.plugin.actions.CodeTimeToggleStatusAction")
    @ActionRegistration(displayName = "not-used", lazy = false)
    public static class CodeTimeToggleStatusAction extends AbstractAction implements Presenter.Menu {

        @Override
        public void actionPerformed(ActionEvent e) {
            SoftwareUtil.toggleStatusBar(UIInteractionType.keyboard);

            SwingUtilities.invokeLater(() -> {
                try {
                    CodeTimeTreeTopComponent.updateMetrics(null, null);
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            });
        }

        @Override
        public JMenuItem getMenuPresenter() {
            String toggleText = "Hide status bar metrics";
            if (!SoftwareUtil.showingStatusText()) {
                toggleText = "Show status bar metrics";
            }
            JMenuItem item = new JMenuItem(toggleText);
            item.addActionListener(this);
            return item;
        }
    }
    

}