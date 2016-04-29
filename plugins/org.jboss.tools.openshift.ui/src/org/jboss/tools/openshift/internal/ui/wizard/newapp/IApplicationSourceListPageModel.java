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
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.wizard.common.IProjectPageModel;

/**
 * Page model that allows retrieval and selection of templates and imagestreams
 * to be used as the source for creating resources to support a microservice
 * deployed to OpenShift, a.k.a an application
 * 
 * @author jeff.cantrill
 *
 */
public interface IApplicationSourceListPageModel extends IProjectPageModel<Connection> {
	
	static final String PROPERTY_SELECTED_APP_SOURCE = "selectedAppSource";
	static final String PROPERTY_APP_SOURCES = "appSources";
	static final String PROPERTY_SERVER_APP_SOURCE = "serverAppSource";
	static final String PROPERTY_LOCAL_APP_SOURCE_FILENAME = "localAppSourceFileName";
	static final String PROPERTY_USE_LOCAL_APP_SOURCE = "useLocalAppSource";
	static final String PROPERTY_ECLIPSE_PROJECT = "eclipseProject";
	
	/**
	 * Sets the selected server app source
	 *
	 * @param appSource
	 */
	void setServerAppSource(IApplicationSource appSource);
	
	/**
	 * Returns the selected server application source
	 *
	 * @return
	 */
	IApplicationSource getServerAppSource();
	
	/**
	 * Returns the selected application source (e.g. template, imagestream)
	 *
	 * @return
	 */
	IApplicationSource getSelectedAppSource();

	/**
	 * Sets this model to use a local app source if <code>true</code> is given. 
	 * Will use server provided app source otherwise.
	 *
	 * @param uploadTemplate
	 */
	void setUseLocalAppSource(boolean uploadAppSource);
	
	/**
	 * Returns <code>true</code> if this model is set use a local app source and upload it.
	 *
	 * @return
	 */
	boolean isUseLocalAppSource();
	
	/**
	 * Sets the name of the local app source that will be uploaded to the server.
	 *
	 * @param name
	 */
	void setLocalAppSourceFileName(String name);
	
	/**
	 * Returns the name of the local app source that will be uploaded to the server.
	 *
	 * @return
	 */
	String getLocalAppSourceFileName();
	
	/**
	 * The collection of observable application sources from
	 * which a new app can be created (e.g. template, imagestream)
	 * @return
	 */
	List<ObservableTreeItem> getAppSources();

	org.eclipse.core.resources.IProject getEclipseProject();

	void setEclipseProject(org.eclipse.core.resources.IProject eclipseProject);
	
}
