/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.software.codetime.models;

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import swdc.java.ops.manager.UtilManager;

/**
 *
 * @author xavierluiz
 */
public class NetbeansProject implements Project {

    @Override
    public FileObject getProjectDirectory() {
        return new NetbeansFileObject(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

    @Override
    public Lookup getLookup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
