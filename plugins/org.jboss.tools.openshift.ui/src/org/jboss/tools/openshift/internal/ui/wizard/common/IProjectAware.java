/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.util.List;

import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IProject;

/**
 * @author Jeff Maury
 */
public interface IProjectAware
{

	public static final String PROPERTY_PROJECT = "project";
	
	public static final String PROPERTY_PROJECT_ITEMS = "projectItems";

	IProject getProject();

	void setProject(IProject project);
	
	List<ObservableTreeItem> getProjectItems();

	boolean hasProjects();
}