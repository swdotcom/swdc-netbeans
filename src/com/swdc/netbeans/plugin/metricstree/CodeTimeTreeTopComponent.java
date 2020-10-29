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
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
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
    
    private static final Map<String, List<ExpandState>> expandStateMap = new HashMap<>();


    public CodeTimeTreeTopComponent() {
        initComponents();
        setName(Bundle.CTL_CodeTimeTreeWindowTopComponent());
        setToolTipText(Bundle.HINT_CodeTimeTreeWindowTopComponent());
       

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Code Time");
        
        List<MetricTreeNode> menuNodes = new ArrayList<>();
        
        menuNodes.addAll(TreeHelper.buildSignupNodes());
        menuNodes.addAll(TreeHelper.buildMenuNodes());
        
        DefaultMutableTreeNode menuRoot = new DefaultMutableTreeNode("Menu");
        DefaultMutableTreeNode dailyMetrics = new DefaultMutableTreeNode("Daily Metrics");
        
        for (MetricTreeNode node : menuNodes) {
            menuRoot.add(node);
        }
        root.add(menuRoot);
        
        CodeTimeSummary codeTimeSummary = TimeDataManager.getCodeTimeSummary();
        SessionSummary sessionSummary = SessionDataManager.getSessionSummaryData();
        Map<String, FileChangeInfo> fileChangeInfoMap = FileAggregateDataManager.getFileChangeInfo();
        
        MetricTreeNode codeTimeTreeNode = TreeHelper.buildCodeTimeTree(codeTimeSummary);
        dailyMetrics.add(codeTimeTreeNode);
        dailyMetrics.add(TreeHelper.buildActiveCodeTimeTree(codeTimeSummary, sessionSummary));
        dailyMetrics.add(TreeHelper.buildLinesAddedTree(sessionSummary));
        dailyMetrics.add(TreeHelper.buildLinesRemovedTree(sessionSummary));
        dailyMetrics.add(TreeHelper.buildKeystrokesTree(sessionSummary));
        dailyMetrics.add(TreeHelper.buildTopKeystrokesFilesTree(fileChangeInfoMap));
        dailyMetrics.add(TreeHelper.buildTopKpmFilesTree(fileChangeInfoMap));
        dailyMetrics.add(TreeHelper.buildTopCodeTimeFilesTree(fileChangeInfoMap));
        
        root.add(dailyMetrics);
        
        JTree codeTimeTree = new JTree(root);
        
        codeTimeTree.setCellRenderer(new MetricTreeNodeRenderer());
        MetricTreeNodeRenderer renderer = (MetricTreeNodeRenderer) codeTimeTree.getCellRenderer();
        renderer.setBackgroundNonSelectionColor(new Color(0,0,0,0));
        renderer.setBorderSelectionColor(new Color(0,0,0,0));
        
        codeTimeTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) codeTimeTree.getLastSelectedPathComponent();

                if (node == null) return;

                if (node instanceof MetricTreeNode) {
                    TreeHelper.handleClickEvent((MetricTreeNode)node);
                }
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            codeTimeTree.clearSelection();
                        } catch (InterruptedException err){
                            System.err.println(err);
                        }
                    }
                });
            }
        });
        scrollPane.setViewportView(codeTimeTree);
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
}
