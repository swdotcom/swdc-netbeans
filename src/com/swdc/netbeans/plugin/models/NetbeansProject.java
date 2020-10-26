/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.models;

import com.swdc.netbeans.plugin.SoftwareUtil;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author xavierluiz
 */
public class NetbeansProject implements Project {

    @Override
    public FileObject getProjectDirectory() {
        return new NetbeansFileObject(SoftwareUtil.UNNAMED_PROJECT, SoftwareUtil.UNTITLED_FILE);
    }

    @Override
    public Lookup getLookup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
