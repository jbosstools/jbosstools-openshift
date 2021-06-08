/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.view.resources;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftODOProjectExists;
import org.jboss.tools.openshift.reddeer.dialogs.InputDialog;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;

/**
 * 
 * OpenShift Application Explorer Registries implemented with RedDeer.
 * 
 */
public class OpenShiftODORegistries extends AbstractOpenShiftApplicationExplorerItem {
	
	public OpenShiftODORegistries(TreeItem registriesItem) {
		super(registriesItem);
	}

	/**
	 * Gets an OpenShift registry with specified registry name.
	 *  
	 * @param registryName name of a registry
	 * @return OpenShift registry
	 */
	public OpenShiftODORegistry getRegistry(String registryName) {
		select();
		item.expand();
		
		new WaitWhile(new JobIsRunning());
		try {
			return new OpenShiftODORegistry(treeViewerHandler.getTreeItem(item, registryName));
		} catch (RedDeerException ex) {
			return null;
		}
	}

	/**
	 * Gets all registries.
	 * 
	 * @return list of all registries.
	 */
	public List<OpenShiftODORegistry> getAllRegistries() {
		List<OpenShiftODORegistry> registries = new ArrayList<>();
		activateOpenShiftApplicationExplorerView();
		item.select();
		item.expand();
		for (TreeItem treeItem: item.getItems()) {
			registries.add(new OpenShiftODORegistry(treeItem));
		}
		return registries;
	}
	
	public String getName() {
		return item.getText();
	}
}
