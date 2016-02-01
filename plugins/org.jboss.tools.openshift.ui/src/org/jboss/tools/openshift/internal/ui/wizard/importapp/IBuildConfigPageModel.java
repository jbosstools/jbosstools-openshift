/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.util.List;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;

/**
 * @author Andre Dietisheim
 */
public interface IBuildConfigPageModel extends IConnectionAware<Connection> {
	
	public static final String PROPERTY_SELECTED_ITEM = "selectedItem";
	public static final String PROPERTY_BUILDCONFIGS = "buildConfigs";

	public Object getSelectedItem();

	public void setSelectedItem(Object selectedItem);
	
	public IBuildConfig getSelectedBuildConfig();

	public void loadBuildConfigs();

	public List<ObservableTreeItem> getBuildConfigs();

	void setProject(IProject project);

	IProject getProject();

}
