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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

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
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "com.swdc.netbeans.plugin.metricstree.CodeTimeTreeWindowTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
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

    private static final Map<String, List<ExpandState>> expandStateMap = new HashMap<>();
    
    private static MetricTree metricTree;

    public CodeTimeTreeTopComponent() {
        initComponents();
        setName(Bundle.CTL_CodeTimeTreeWindowTopComponent());
        setToolTipText(Bundle.HINT_CodeTimeTreeWindowTopComponent());
        
        metricTree = buildCodeTimeTreeView();

        scrollPane.setViewportView(metricTree);
        scrollPane.setVisible(true);

        this.updateUI();
        this.setVisible(true);
    }

    public static class ExpandState {

        public boolean expand = false;
        public TreePath path = null;

        public ExpandState(boolean expand, TreePath path) {
            this.expand = expand;
            this.path = path;
        }
    }
    
    public static void updateMetrics(CodeTimeSummary codeTimeSummary, SessionSummary sessionSummary) {
        
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
    
    private static void updateNodeLabel(MetricTreeNode node, String label) {
        if (node != null) {
            node.updateLabel(label);
        }
    }
    
    private static void updateNodeIconName(MetricTreeNode node, String iconName) {
        if (node != null) {
            node.updateIconName(iconName);
        }
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
        //
    }

    public static void openTree() {
        Project project = SoftwareUtil.getFirstActiveProject();
        if (project != null) {
            // ToolWindowManager.getInstance(project).getToolWindow("Code Time").show(null);
        }
    }

    public static void updateExpandState(String id, TreePath path, boolean expanded) {
        ExpandState state = new ExpandState(expanded, path);
        List<ExpandState> existingStates = expandStateMap.get(id);
        if (existingStates == null) {
            existingStates = new ArrayList<>();
            existingStates.add(state);
        } else {
            boolean foundExisting = false;
            for (ExpandState s : existingStates) {
                String pathStr = s.path.toString();
                String tPathStr = path.toString();
                if (pathStr.equals(tPathStr)) {
                    s.expand = expanded;
                    foundExisting = true;
                    break;
                }
            }
            if (!foundExisting) {
                existingStates.add(state);
            }
        }

        expandStateMap.put(id, existingStates);
    }

    public static List<ExpandState> getExpandState(String id) {
        return expandStateMap.get(id);
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
        for (MetricTreeNode node : loginNodes) {
            root.add(node);
        }
        
        List<MetricTreeNode> menuNodes = TreeHelper.buildMenuNodes();
        for (MetricTreeNode node : menuNodes) {
            root.add(node);
        }
        
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
}
