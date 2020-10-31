/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.metricstree;

import java.awt.Component;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class MetricTree extends JTree {

    public String id;
    public boolean expandState = false;
    
    public MetricTree(TreeModel model ) {
        super(model);
        
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    MetricTree tree = (MetricTree) e.getSource();
                    if (tree != null) {

                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                        if (node == null) {
                            return;
                        }

                        if (node instanceof MetricTreeNode) {
                            TreeHelper.handleClickEvent((MetricTreeNode) node);
                        }
                    }
                } catch (Exception ex) {
                    //
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                try {
                    MetricTree tree = (MetricTree) e.getSource();
                    if (tree != null) {
                        tree.clearSelection();
                    }
                } catch (Exception ex) {
                    //
                }
            }
        });
    }

    @Override
    public void setExpandedState(TreePath path, boolean state) {
        this.expandState = state;
        super.setExpandedState(path, state);
    }

    @Override
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
    }

    public boolean isExpandState() {
        return expandState;
    }

    @Override
    public TreeCellRenderer getCellRenderer() {
        return super.getCellRenderer();
    }

    public Component add(String name, String id) {
        this.id = id;
        Component comp = new Component() {
            @Override
            public void setName(String name) {
                super.setName(name);
            }
        };
        comp.setName(name);
        return super.add(comp);
    }
    


}
