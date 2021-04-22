/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Project;
import swdc.java.ops.providers.IdeProject;

/**
 *
 * @author xavierluiz
 */
public class NetbeansProject implements IdeProject {

    @Override
    public Project getProjectForPath(String string) {
        File f = new File(string);
        org.netbeans.api.project.Project p = FileOwnerQuery.getOwner(f.toURI());
        return createProject(p);
    }

    @Override
    public Project getFirstActiveProject() {
        Set<org.netbeans.api.project.Project> modifiedProjects = ProjectManager.getDefault().getModifiedProjects();
        if (modifiedProjects.size() > 0) {
            // return the 1st one
            return createProject(modifiedProjects.iterator().next());
        }
        
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc != null) {
            Document d = jtc.getDocument();
            return createProjectFromDocument(d);
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

    @Override
    public Project getOpenProject() {
        return getFirstActiveProject();
    }

    @Override
    public Project buildKeystrokeProject(Object o) {
        return createProject((org.netbeans.api.project.Project)o);
    }

    @Override
    public String getFileSyntax(File file) {
        if (file == null) {
            return "";
        }
        
        Path path = file.toPath();
        try {
            return Files.probeContentType(path);
        } catch (Exception e) {
            
        }
        String fullFileName = file.getAbsolutePath();
        return (fullFileName.contains(".")) ? fullFileName.substring(fullFileName.lastIndexOf(".") + 1) : "";
    }
    
    private Project createProjectFromDocument(Document d) {
        Source source = Source.create(d);
        if (source == null) {
            return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
        }
        FileObject fileObject = source.getFileObject();
        if (fileObject == null) {
            return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
        }
        return createProject(FileOwnerQuery.getOwner(fileObject));
    }
    
    private Project createProject(org.netbeans.api.project.Project p) {
        if (p != null) {
            return new Project(p.getProjectDirectory().getName(), p.getProjectDirectory().getPath());
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }
    
}
