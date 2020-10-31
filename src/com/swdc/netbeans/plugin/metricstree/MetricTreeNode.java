/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class MetricTreeNode extends DefaultMutableTreeNode {

    protected DefaultTreeModel model;

    private String id;
    private String iconName;
    private Object data;
    private boolean expanded = false;
    private boolean leaf = false;

    public MetricTreeNode(String label, boolean isLeaf) {
        super(label);
        this.init(label, null, isLeaf);
    }

    public MetricTreeNode(String label, String iconName, boolean isLeaf) {
        super(label);
        this.init(label, iconName, isLeaf);
    }

    public MetricTreeNode(String label, String iconName, String id, boolean isLeaf) {
        super(label);
        this.init(id, iconName, isLeaf);
    }
    
    private void init(String id, String iconName, boolean isLeaf) {
        this.id = id;
        this.iconName = iconName;
        this.leaf = isLeaf;
        this.expanded = !isLeaf;
        this.initModel();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isLeaf() {
        return leaf;
    }

    private void initModel() {
        DefaultTreeModel parentNodeModel = new DefaultTreeModel(this);
        this.setModel(parentNodeModel);
    }

    public void setModel(DefaultTreeModel model) {
        this.model = model;
    }

    public void add(MutableTreeNode node) {
        super.add(node);
        nodeWasAdded(this, getChildCount() - 1);
    }

    protected void nodeWasAdded(TreeNode node, int index) {
        if (model == null) {
            ((MetricTreeNode) node.getParent()).nodeWasAdded(node, index);
        } else {
            int[] childIndices = new int[1];
            childIndices[0] = index;
            model.nodesWereInserted(node, childIndices);
        }
    }

    public String getId() {
        return id;
    }

    public String getIconName() {
        return iconName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object obj) {
        this.data = obj;
    }

    public TreePath getNodeTreePath() {
        TreePath p = new TreePath(model.getPathToRoot(this));
        return p;
    }
}
