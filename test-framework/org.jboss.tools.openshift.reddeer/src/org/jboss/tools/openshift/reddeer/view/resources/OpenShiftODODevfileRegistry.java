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
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.ODORegistryIsDeleted;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * OpenShift Application Explorer Registry implemented with
 * RedDeer.
 * 
 */
public class OpenShiftODODevfileRegistry extends AbstractOpenShiftApplicationExplorerItem {

	private String registryName;

	public OpenShiftODODevfileRegistry(TreeItem registryItem) {
		super(registryItem);
		this.registryName = treeViewerHandler.getNonStyledText(item);
	}

	public String getName() {
		return registryName;
	}

	/**
	 * Gets an OpenShift registry with specified registry name.
	 * 
	 * @param devfileName name of a devfile
	 * @return OpenShift devfile
	 */
	public OpenShiftODODevfile getDevfile(String devfileName) {
		select();
		item.expand();

		new WaitWhile(new JobIsRunning());
		try {
			return new OpenShiftODODevfile(treeViewerHandler.getTreeItem(item, devfileName));
		} catch (RedDeerException ex) {
			return null;
		}
	}

	/**
	 * Gets all registries existing on a connection.
	 * 
	 * @return list of all projects for a connection.
	 */
	public List<OpenShiftODODevfile> getAllDevfiles() {
		List<OpenShiftODODevfile> devfiles = new ArrayList<>();
		activateOpenShiftApplicationExplorerView();
		item.select();
		item.expand();
		for (TreeItem treeItem : item.getItems()) {
			devfiles.add(new OpenShiftODODevfile(treeItem));
		}
		return devfiles;
	}

	/**
	 * Deletes OpenShift registry.
	 */
	public void delete() {
		item.select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_OS_PROJECT).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_REGISTRY + " " + getName());
		new OkButton().click();
		
		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_REGISTRY + " " + getName()), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(new Matcher[]{CoreMatchers.is(OpenShiftLabel.JobsLabels.DELETE)}), TimePeriod.LONG);
		new WaitUntil(new ODORegistryIsDeleted(getName()), TimePeriod.getCustom(120));
	}

}
