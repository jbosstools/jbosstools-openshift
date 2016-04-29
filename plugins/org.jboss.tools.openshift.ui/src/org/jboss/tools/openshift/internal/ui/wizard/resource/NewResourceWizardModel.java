/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.jboss.tools.common.databinding.ObservablePojo;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import com.openshift.restclient.model.IProject;

/**
 * @author Jeff Maury
 */
public class NewResourceWizardModel extends ObservablePojo implements IResourcePayloadPageModel {

    private IConnection connection;
    private IProject project;
    private List<ObservableTreeItem> projectItems = new ArrayList<>();
    private String source;

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.common.IProjectPageModel#loadResources()
     */
    @Override
    public void loadResources() {
        if (connection == null) {
            return;
        }
        ObservableTreeItem connectionItem = ProjectTreeItems.INSTANCE.create(connection);
        connectionItem.load();
        List<ObservableTreeItem> projects = connectionItem.getChildren();
        setProjectItems(projects);
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware#getConnection()
     */
    @Override
    public IConnection getConnection() {
        return connection;
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware#hasConnection()
     */
    @Override
    public boolean hasConnection() {
        return connection != null;
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware#setConnection(org.jboss.tools.openshift.common.core.connection.IConnection)
     */
    @Override
    public void setConnection(IConnection connection) {
        if (ObjectUtils.equals(connection, this.connection)) {
            return;
        }
        firePropertyChange(PROPERTY_CONNECTION, this.connection, this.connection = connection);
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware#getContext()
     */
    @Override
    public Object getContext() {
        return null;
    }

    private void update(IProject project, List<ObservableTreeItem> projectItems, String source) {
        updateProjectItems(projectItems);
        firePropertyChange(PROPERTY_PROJECT, this.project, this.project = getDefaultProject(project, projectItems));
        firePropertyChange(PROPERTY_SOURCE, this.source, this.source = source);
    }
    
    private IProject getDefaultProject(IProject project, List<ObservableTreeItem> projectItems) {
        if ((project == null) && (projectItems != null) && (projectItems.size() > 0)) {
            project = (IProject) projectItems.get(0).getModel();
        }
        return project;
    }
    
    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.common.IProjectAware#getProject()
     */
    @Override
    public IProject getProject() {
        //System.out.println("getProject return " + project.hashCode());
        return project;
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.common.IProjectAware#setProject(com.openshift.restclient.model.IProject)
     */
    @Override
    public void setProject(IProject project) {
        update(project, this.projectItems, this.source);
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.common.IProjectAware#getProjectItems()
     */
    @Override
    public List<ObservableTreeItem> getProjectItems() {
        return projectItems;
    }
    
    private void updateProjectItems(List<ObservableTreeItem> projectItems) {
        List<ObservableTreeItem> oldItems = new ArrayList<>(this.projectItems);
        // ensure we're not operating on the same list
        List<ObservableTreeItem> newItems = new ArrayList<>();
        if (projectItems != null) {
            newItems.addAll(projectItems);
        }
        this.projectItems.clear();
        this.projectItems.addAll(newItems);
        firePropertyChange(PROPERTY_PROJECT_ITEMS, oldItems, this.projectItems);
    }
    
    private IProject checkProject(IProject project, List<ObservableTreeItem> projects) {
        if (projects != null) {
            return (IProject) projects.stream().map(o -> o.getModel())
            .filter(model -> ObjectUtils.equals(project, model))
            .findFirst()
            .orElse(null);
        } else {
            return null;
        }
    }
    public void setProjectItems(List<ObservableTreeItem> projectItems) {
        update(checkProject(this.project, projectItems), projectItems, this.source);
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.common.IProjectAware#hasProjects()
     */
    @Override
    public boolean hasProjects() {
        return projectItems != null && !projectItems.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.resource.IResourcePayloadPageModel#setLocalSourceFileName(java.lang.String)
     */
    @Override
    public void setSource(String source) {
        update(this.project, this.projectItems, source);
   }

    /* (non-Javadoc)
     * @see org.jboss.tools.openshift.internal.ui.wizard.resource.IResourcePayloadPageModel#getLocalSourceFileName()
     */
    @Override
    public String getSource() {
        return source;
    }

}
