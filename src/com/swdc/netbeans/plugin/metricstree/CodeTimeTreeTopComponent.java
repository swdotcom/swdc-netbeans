/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.netbeans.plugin.managers.FileAggregateDataManager;
import com.swdc.netbeans.plugin.managers.FileManager;
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
    
    private static MetricTree metricTree;
    private static boolean expandInitialized = false;

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
        String name = FileManager.getItem("name");
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
            String authType = FileManager.getItem("authType");
            String iconName = "icons8-envelope-16.png";
            if ("google".equals(authType)) {
                iconName = "google.png";
            } else if ("github".equals(authType)) {
                iconName = "github.png";
            }
            
            String email = FileManager.getItem("name");
            // update the logged in node
            updateNodeLabel(findNodeById(TreeHelper.LOGGED_IN_ID), email, iconName);
        }
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        
        updateMetrics(codeTimeSummary, sessionSummary);
        
        updateTopFileMetrics(fileChangeInfoMap);
    }
    
    public static void rebuildTree() {
        try {
            expandInitialized = false;
            CodeTimeTreeTopComponent topComp = (CodeTimeTreeTopComponent) WindowManager.getDefault().findTopComponent("CodeTimeTreeWindowTopComponent");
            if (topComp != null) {
                topComp.init();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to retrieve the code time top component: {0}", e.getMessage());
        }
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
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
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

    private MetricTree buildCodeTimeTreeView() {
        MetricTree tree = new MetricTree(makeMetricNodeModel());

        tree.setCellRenderer(new IconTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);

        return tree;
    }
    
    private TreeModel makeMetricNodeModel() {
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
        
        root.add(new MetricTreeNode(true));
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        
        MetricLabels mLabels = new MetricLabels();
        mLabels.updateLabels(codeTimeSummary, sessionSummary);

        root.add(TreeHelper.buildCodeTimeTree(mLabels));
        root.add(TreeHelper.buildActiveCodeTimeTree(mLabels));
        root.add(TreeHelper.buildLinesAddedTree(mLabels));
        root.add(TreeHelper.buildLinesRemovedTree(mLabels));
        root.add(TreeHelper.buildKeystrokesTree(mLabels));
        
        if (fileChangeInfoMap != null) {
            root.add(TreeHelper.buildTopKeystrokesFilesTree(fileChangeInfoMap));
            root.add(TreeHelper.buildTopKpmFilesTree(fileChangeInfoMap));
            root.add(TreeHelper.buildTopCodeTimeFilesTree(fileChangeInfoMap));
        }
        
        return new DefaultTreeModel(root);
    }
    
    private static void updateTopFileMetrics(Map<String, FileChangeInfo> fileChangeInfoMap) {
        // get the 3 parents: keystrokes, kpm, codetime
        MetricTreeNode parent = findNodeById(TreeHelper.getTopFileParentId("keystrokes"));
        if (parent != null) {
            parent.removeAllChildren();
            TreeHelper.addNodesToTopFilesMetricParentTreeNode(parent, "keystrokes", fileChangeInfoMap);
        }
        
        parent = findNodeById(TreeHelper.getTopFileParentId("kpm"));
        if (parent != null) {
            parent.removeAllChildren();
            TreeHelper.addNodesToTopFilesMetricParentTreeNode(parent, "kpm", fileChangeInfoMap);
        }
        
        parent = findNodeById(TreeHelper.getTopFileParentId("codetime"));
        if (parent != null) {
            parent.removeAllChildren();
            TreeHelper.addNodesToTopFilesMetricParentTreeNode(parent, "codetime", fileChangeInfoMap);
        }
    }
    
    public static void updateMetrics(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        if (metricTree != null) {
            // update the toggle node label
            String toggleText = "Hide status bar metrics";
            if (!SoftwareUtil.showingStatusText()) {
                toggleText = "Show status bar metrics";
            }

            updateNodeLabel(findNodeById(TreeHelper.TOGGLE_METRICS_ID), toggleText);

            MetricLabels mLabels = new MetricLabels();
            mLabels.updateLabels(codeTimeSummary, sessionSummary);

            if (codeTimeSummary != null && sessionSummary != null) {
                updateNodeLabel(findNodeById(TreeHelper.ACTIVE_CODETIME_GLOBAL_AVG_TODAY_ID), mLabels.activeCodeTimeGlobalAvg);

                MetricTreeNode activeCodeTimeAvgNode = findNodeById(TreeHelper.ACTIVE_CODETIME_AVG_TODAY_ID);
                updateNodeLabel(activeCodeTimeAvgNode, mLabels.activeCodeTimeAvg);
                updateNodeIconName(activeCodeTimeAvgNode, mLabels.activeCodeTimeAvgIcon);

                updateNodeLabel(findNodeById(TreeHelper.ACTIVE_CODETIME_TODAY_ID), mLabels.activeCodeTime);

                updateNodeLabel(findNodeById(TreeHelper.CODETIME_TODAY_ID), mLabels.codeTime);
            }

            if (sessionSummary != null) {
                // all of the other metrics can be updated
                // LINES DELETED
                updateNodeLabel(findNodeById(TreeHelper.LINES_DELETED_GLOBAL_AVG_TODAY_ID), mLabels.linesRemovedGlobalAvg);

                MetricTreeNode linesDeletedAvgNode = findNodeById(TreeHelper.LINES_DELETED_AVG_TODAY_ID);
                updateNodeLabel(linesDeletedAvgNode, mLabels.linesRemovedAvg);
                updateNodeIconName(linesDeletedAvgNode, mLabels.linesRemovedAvgIcon);

                updateNodeLabel(findNodeById(TreeHelper.LINES_DELETED_TODAY_ID), mLabels.linesRemoved);

                // LINES ADDED
                updateNodeLabel(findNodeById(TreeHelper.LINES_ADDED_GLOBAL_AVG_TODAY_ID), mLabels.linesAddedGlobalAvg);

                MetricTreeNode linesAddedAvgNode = findNodeById(TreeHelper.LINES_ADDED_AVG_TODAY_ID);
                updateNodeLabel(linesAddedAvgNode, mLabels.linesAddedAvg);
                updateNodeIconName(linesAddedAvgNode, mLabels.linesAddedAvgIcon);

                updateNodeLabel(findNodeById(TreeHelper.LINES_ADDED_TODAY_ID), mLabels.linesAdded);

                // KEYSTROKES
                updateNodeLabel(findNodeById(TreeHelper.KEYSTROKES_GLOBAL_AVG_TODAY_ID), mLabels.keystrokesGlobalAvg);

                MetricTreeNode keystrokesAvgNode = findNodeById(TreeHelper.KEYSTROKES_AVG_TODAY_ID);
                updateNodeLabel(keystrokesAvgNode, mLabels.keystrokesAvg);
                updateNodeIconName(keystrokesAvgNode, mLabels.keystrokesAvgIcon);

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
}
