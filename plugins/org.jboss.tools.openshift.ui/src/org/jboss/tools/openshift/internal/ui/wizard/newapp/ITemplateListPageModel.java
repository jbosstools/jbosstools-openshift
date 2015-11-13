/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.ITemplate;

/**
 * Page model that allows retrieval and selection of templates,
 * 
 * @author jeff.cantrill
 *
 */
public interface ITemplateListPageModel extends IConnectionAware<Connection> {
	
	static final String PROPERTY_PROJECT = "project";
	static final String PROPERTY_PROJECT_ITEMS = "projectItems";
	static final String PROPERTY_SELECTED_TEMPLATE = "selectedTemplate";
	static final String PROPERTY_TEMPLATES = "templates";
	static final String PROPERTY_SERVER_TEMPLATE = "serverTemplate";
	static final String PROPERTY_LOCAL_TEMPLATE_FILENAME = "localTemplateFileName";
	static final String PROPERTY_USE_LOCAL_TEMPLATE = "useLocalTemplate";
	static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	
	/**
	 * Loads the resources for the given connection (in this model) from the server. Should be
	 * called from a job (so that UI wont freeze)
	 */
	void loadResources();
	
	/**
	 * Sets the selected template
	 *
	 * @param template
	 */
	void setServerTemplate(ITemplate template);
	
	/**
	 * Returns the selected server template
	 *
	 * @return
	 */
	ITemplate getServerTemplate();
	
	/**
	 * Returns the selected server template
	 *
	 * @return
	 */
	ITemplate getSelectedTemplate();

	/**
	 * Sets this model to use a local template if <code>true</code> is given. 
	 * Will use server provided template otherwise.
	 *
	 * @param uploadTemplate
	 */
	void setUseLocalTemplate(boolean uploadTemplate);
	
	/**
	 * Returns <code>true</code> if this model is set use a local template and upload it.
	 *
	 * @return
	 */
	boolean isUseLocalTemplate();
	
	/**
	 * Sets the name of the local template that will be uploaded to the server.
	 *
	 * @param name
	 */
	void setLocalTemplateFileName(String name);
	
	/**
	 * Returns the name of the local template that will be uploaded to the server.
	 *
	 * @return
	 */
	String getLocalTemplateFileName();
	
	void setProject(IProject project);

	IProject getProject();
	
	List<ObservableTreeItem> getProjectItems();

	boolean hasProjects();

	List<ObservableTreeItem> getTemplates();

	org.eclipse.core.resources.IProject getEclipseProject();

	void setEclipseProject(org.eclipse.core.resources.IProject eclipseProject);
	
}
