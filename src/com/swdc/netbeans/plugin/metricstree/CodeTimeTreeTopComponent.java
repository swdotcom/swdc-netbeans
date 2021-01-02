/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileAggregateDataManager;
import com.swdc.netbeans.plugin.managers.SessionDataManager;
import com.swdc.netbeans.plugin.managers.TimeDataManager;
import com.swdc.netbeans.plugin.models.CodeTimeSummary;
import com.swdc.netbeans.plugin.models.FileChangeInfo;
import com.swdc.netbeans.plugin.models.SessionSummary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.SlackManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//com.swdc.netbeans.plugin.metricstree//CodeTimeTreeWindow//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "CodeTimeTreeWindowTopComponent",
        iconBase = "com/swdc/netbeans/plugin/assets/paw.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Tools", id = "com.swdc.netbeans.plugin.metricstree.CodeTimeTreeWindowTopComponent")
@ActionReference(path = "Menu/Tools/Code Time" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_CodeTimeTreeWindowAction",
        preferredID = "CodeTimeTreeWindowTopComponent"
)
@Messages({
    "CTL_CodeTimeTreeWindowAction=Code Time",
    "CTL_CodeTimeTreeWindowTopComponent=Code Time",
    "HINT_CodeTimeTreeWindowTopComponent=This is a Code Time window"
})
public final class CodeTimeTreeTopComponent extends TopComponent {
    
    public static final Logger LOG = Logger.getLogger("CodeTimeTreeTopComponent");
    
    public static MetricTree metricTree;
    private static boolean expandInitialized = false;
    private static long last_refresh_millis = 0;
    private static long last_rebuild_millis = 0;

    public CodeTimeTreeTopComponent() {
        initComponents();
        setName(Bundle.CTL_CodeTimeTreeWindowTopComponent());
        setToolTipText(Bundle.HINT_CodeTimeTreeWindowTopComponent());
        
        this.init();
    }
    
    protected void init() {
        
        metricTree = buildCodeTimeTreeView();
        
        if (!expandInitialized) {
            int activeCodeTimeParentRow = findParentNodeRowById(TreeHelper.ACTIVE_CODETIME_PARENT_ID);
            if (activeCodeTimeParentRow != -1) {
                metricTree.expandRow(activeCodeTimeParentRow);
            }
            int codeTimeParentRow = findParentNodeRowById(TreeHelper.CODETIME_PARENT_ID);
            if (codeTimeParentRow != -1) {
                metricTree.expandRow(codeTimeParentRow);
            }
            int loggedInParentRow = findParentNodeRowById(TreeHelper.LOGGED_IN_ID);
            if (loggedInParentRow != -1) {
                metricTree.expandRow(loggedInParentRow);
            }
            expandInitialized = true;
        }

        scrollPane.setViewportView(metricTree);
        scrollPane.setVisible(true);

        this.updateUI();
        this.setVisible(true);
    }
    
    private static void updateNodeLabel(MetricTreeNode node, String label) {
        updateNodeLabel(node, label, null);
    }

    private static void updateNodeLabel(MetricTreeNode node, String label, String iconName) {
        if (node != null) {
            if (iconName != null) {
                node.updateIconName(iconName);
            }
            node.updateLabel(label);
        }
    }
    
    private static void updateNodeIconName(MetricTreeNode node, String iconName) {
        if (node != null) {
            node.updateIconName(iconName);
        }
    }
    
    private static int findParentNodeRowById(String id) {
        int row = 0;
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();
            
            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            return row;
                        }
                        row++;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }
        
        return -1; 
    }
    
    private static MetricTreeNode findNodeById(String id) {
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();
            
            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            return node;
                        } else if (node != null && node.getChildCount() > 0) {
                            // check its children
                            for (int i = 0; i < node.getChildCount(); i++) {
                                MetricTreeNode childNode = (MetricTreeNode) node.getChildAt(i);
                                if (childNode != null && childNode.getId().equals(id)) {
                                    return childNode;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }
        
        return null;
    }

    public static void refreshTree() {
        SwingUtilities.invokeLater(() -> {
            try {
                // refresh if metric tree has been initialized
                if (metricTree != null && (last_refresh_millis == 0 || System.currentTimeMillis() - last_refresh_millis > 3000)) {
                    last_refresh_millis = System.currentTimeMillis();
                    refresh();
                }
            } catch (Exception e) {
                System.err.println(e);
            }
        });
    }
    
    private static void refresh() {
        rebuildMenuNodes();
        
        rebuildFlowNodes();
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        
        updateMetrics(codeTimeSummary, sessionSummary);
    }
    
    public static void rebuildTree() {
        SwingUtilities.invokeLater(() -> {
            try {
                expandInitialized = false;
                CodeTimeTreeTopComponent topComp = (CodeTimeTreeTopComponent) WindowManager.getDefault().findTopComponent("CodeTimeTreeWindowTopComponent");
                if (topComp != null && System.currentTimeMillis() - last_rebuild_millis > 3000) {
                    last_rebuild_millis = System.currentTimeMillis();
                    topComp.init();
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to retrieve the code time top component: {0}", e.getMessage());
            }
        });
    }

    public static void openTree() {
        try {
            CodeTimeTreeTopComponent topComp = (CodeTimeTreeTopComponent) WindowManager.getDefault().findTopComponent("CodeTimeTreeWindowTopComponent");
            if (topComp != null) {
                topComp.open();
                topComp.updateUI();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve the code time top component: {0}", e.getMessage());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
    
    private MetricTree buildMenuTreeView() {
        MetricTree tree = new MetricTree(makeMenuNodeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }
    
    private MetricTree buildFlowTreeView() {
        MetricTree tree = new MetricTree(makeFlowNodeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }
    
    private MetricTree buildKpmTreeView() {
        MetricTree tree = new MetricTree(makeKpmNodeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }

    private MetricTree buildCodeTimeTreeView() {
        MetricTree tree = new MetricTree(makeCodetimeTreeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }
    
    private TreeModel makeMenuNodeModel() {
        // "Root" will not be visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        
        List<MetricTreeNode> loginNodes = TreeHelper.buildSignupNodes();
        loginNodes.forEach(node -> {
            root.add(node);
        });
        
        List<MetricTreeNode> menuNodes = TreeHelper.buildMenuNodes();
        menuNodes.forEach(node -> {
            root.add(node);
        });
        return new DefaultTreeModel(root);
    }
    
    private TreeModel makeFlowNodeModel() {
        // "Root" will not be visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        
        List<MetricTreeNode> flowNodes = TreeHelper.buildTreeFlowNodes();
        flowNodes.forEach(node -> {
            root.add(node);
        });
        return new DefaultTreeModel(root);
    }
    
    private TreeModel makeKpmNodeModel() {
        // "Root" will not be visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        
        MetricLabels mLabels = new MetricLabels();
        mLabels.updateLabels(codeTimeSummary, sessionSummary);

        root.add(TreeHelper.buildCodeTimeTree(mLabels));
        root.add(TreeHelper.buildActiveCodeTimeTree(mLabels));
        root.add(TreeHelper.buildLinesAddedTree(mLabels));
        root.add(TreeHelper.buildLinesRemovedTree(mLabels));
        root.add(TreeHelper.buildKeystrokesTree(mLabels));
        
        root.add(TreeHelper.buildSummaryButton());
        root.add(TreeHelper.buildViewWebDashboardButton());
        return new DefaultTreeModel(root);
    }
    
    private TreeModel makeCodetimeTreeModel() {
        // "Root" will not be visible
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        
        List<MetricTreeNode> loginNodes = TreeHelper.buildSignupNodes();
        loginNodes.forEach(node -> {
            root.add(node);
        });
        
        List<MetricTreeNode> menuNodes = TreeHelper.buildMenuNodes();
        menuNodes.forEach(node -> {
            root.add(node);
        });
        
        root.add(new MetricTreeNode(true /*isSeparator*/));
        
        List<MetricTreeNode> flowNodes = TreeHelper.buildTreeFlowNodes();
        flowNodes.forEach(node -> {
            root.add(node);
        });
        
        root.add(new MetricTreeNode(true /*isSeparator*/));
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        
        MetricLabels mLabels = new MetricLabels();
        mLabels.updateLabels(codeTimeSummary, sessionSummary);

        root.add(TreeHelper.buildCodeTimeTree(mLabels));
        root.add(TreeHelper.buildActiveCodeTimeTree(mLabels));
        root.add(TreeHelper.buildLinesAddedTree(mLabels));
        root.add(TreeHelper.buildLinesRemovedTree(mLabels));
        root.add(TreeHelper.buildKeystrokesTree(mLabels));
        
        root.add(TreeHelper.buildSummaryButton());
        root.add(TreeHelper.buildViewWebDashboardButton());
        
        return new DefaultTreeModel(root);
    }
    
    public static void updateMetrics(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        if (metricTree != null) {

            MetricLabels mLabels = new MetricLabels();
            mLabels.updateLabels(codeTimeSummary, sessionSummary);

            if (codeTimeSummary != null && sessionSummary != null) {
                updateNodeLabel(findNodeById(TreeHelper.ACTIVE_CODETIME_TODAY_ID), mLabels.activeCodeTime);

                updateNodeLabel(findNodeById(TreeHelper.CODETIME_TODAY_ID), mLabels.codeTime);
            }

            if (sessionSummary != null) {
                // all of the other metrics can be updated
                // LINES DELETED
                updateNodeLabel(findNodeById(TreeHelper.LINES_DELETED_TODAY_ID), mLabels.linesRemoved);

                // LINES ADDED
                updateNodeLabel(findNodeById(TreeHelper.LINES_ADDED_TODAY_ID), mLabels.linesAdded);

                // KEYSTROKES
                updateNodeLabel(findNodeById(TreeHelper.KEYSTROKES_TODAY_ID), mLabels.keystrokes);
            }

            metricTree.updateUI();
        }
    }
    
    public static void expandCollapse(String id) {
        int row = 0;
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            if (!node.isExpanded()) {
                                metricTree.expandRow(row);
                                node.setExpanded(true);
                            } else {
                                metricTree.collapseRow(row);
                                node.setExpanded(false);
                            }
                            break;
                        }
                        row++;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Find node by ID error: {0}", e.toString());
        }
    }
    
    private static void removeNodeById(String id) {
        try {
            DefaultTreeModel model = (DefaultTreeModel) metricTree.getModel();

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) model.getRoot();

            if (treeNode != null) {
                Enumeration<TreeNode> nodes = treeNode.children();
                if (nodes != null) {
                    while (nodes.hasMoreElements()) {
                        MetricTreeNode node = (MetricTreeNode) nodes.nextElement();
                        if (node != null && node.getId().equals(id)) {
                            treeNode.remove(node);
                            return;
                        } else if (node != null && node.getChildCount() > 0) {
                            // check its children
                            for (int i = 0; i < node.getChildCount(); i++) {
                                MetricTreeNode childNode = (MetricTreeNode) node.getChildAt(i);
                                if (childNode != null && childNode.getId().equals(id)) {
                                    treeNode.remove(childNode);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "Remove node by ID error: {0}", e.toString());
        }
    }
    
    private static void rebuildMenuNodes() {
        String name = FileUtilManager.getItem("name");
        // check to see if we need to swap out the signup nodes with the signed up node
        MetricTreeNode loggedInNode = findNodeById(TreeHelper.LOGGED_IN_ID);
        if (StringUtils.isNotBlank(name) && loggedInNode == null) {
            // swap the nodes out
            removeNodeById(TreeHelper.EMAIL_SIGNUP_ID);
            removeNodeById(TreeHelper.GITHIUB_SIGNUP_ID);
            removeNodeById(TreeHelper.GOOGLE_SIGNUP_ID);

            // add the LOGGED_IN_ID node
            loggedInNode = TreeHelper.buildLoggedInNode();
            ((DefaultMutableTreeNode)metricTree.getModel().getRoot()).insert(loggedInNode, 0);
        } else {
            String authType = FileUtilManager.getItem("authType");
            String iconName = "icons8-envelope-16.png";
            if ("google".equals(authType)) {
                iconName = "google.png";
            } else if ("github".equals(authType)) {
                iconName = "github.png";
            }
            
            String email = FileUtilManager.getItem("name");
            // update the logged in node
            updateNodeLabel(findNodeById(TreeHelper.LOGGED_IN_ID), email, iconName);
        }
        
        // update the toggle node label
        String toggleText = "Hide status bar metrics";
        if (!SoftwareUtil.showingStatusText()) {
            toggleText = "Show status bar metrics";
        }

        updateNodeLabel(findNodeById(TreeHelper.TOGGLE_METRICS_ID), toggleText);
    }
    
    private static void rebuildFlowNodes() {
        MetricTreeNode connectSlackId = findNodeById(TreeHelper.CONNECT_SLACK_ID);
        if (connectSlackId != null) {
            if (SlackManager.hasSlackWorkspaces()) {
                // remove this node
                removeNodeById(TreeHelper.CONNECT_SLACK_ID);
            }
        }
    }
}
