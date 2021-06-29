/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
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
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.wizard.CreateDevfileRegistryWizard;
import org.jboss.tools.openshift.reddeer.wizard.page.CreateDevfileRegistryWizadPage;

/**
 * 
 * OpenShift Application Explorer Registries implemented with RedDeer.
 * 
 * @author jkopriva@redhat.com
 *
 */
public class OpenShiftODODevfileRegistries extends AbstractOpenShiftApplicationExplorerItem {

	public OpenShiftODODevfileRegistries(TreeItem connectionItem) {
		super(connectionItem);
	}

	/**
	 * Gets an OpenShift registry with specified registry name.
	 * 
	 * @param registryName name of a registry, if displayed name is provided, its
	 *                     displayed name, otherwise its registry name
	 * @return OpenShift registry
	 */
	public OpenShiftODODevfileRegistry getRegistry(String registryName) {
		select();
		item.expand();

		new WaitWhile(new JobIsRunning());
		try {
			return new OpenShiftODODevfileRegistry(treeViewerHandler.getTreeItem(item, registryName));
		} catch (RedDeerException ex) {
			return null;
		}

	}

	/**
	 * Gets first registry of a OpenShift connection.
	 *
	 * @return OpenShift registry
	 */
	public OpenShiftODODevfileRegistry getDefaultRegistry() {
		return getRegistry("DefaultDevfileRegistry");
	}

	/**
	 * Gets all registries existing on a connection.
	 * 
	 * @return list of all registries for a connection.
	 */
	public List<OpenShiftODODevfileRegistry> getAllRegistries() {
		List<OpenShiftODODevfileRegistry> registries = new ArrayList<OpenShiftODODevfileRegistry>();
		activateOpenShiftApplicationExplorerView();
		item.select();
		item.expand();
		for (TreeItem treeItem : item.getItems()) {
			if (treeItem.getText().contains("Click to login.")) {
				OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
				explorer.activate();
				explorer.connectToOpenShiftODO();
			}
			registries.add(new OpenShiftODODevfileRegistry(treeItem));
		}
		return registries;
	}

	/**
	 * Creates a new OpenShift registry for a connection.
	 * 
	 * @param registryName  registry name
	 * @param displayedName displayed name
	 * @return OpenShift registry
	 */
	public OpenShiftODODevfileRegistry createNewRegistry(String registryName, String registryURL, boolean secure) {
		select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.NEW_REGISTRY).select();
		CreateDevfileRegistryWizard registryWizard = new CreateDevfileRegistryWizard();
		CreateDevfileRegistryWizadPage registryWizardPage = new CreateDevfileRegistryWizadPage(registryWizard);
		registryWizardPage.setRegistryName(registryName);
		registryWizardPage.setURL(registryURL);
		if (secure) {
			registryWizardPage.setSecure(secure);
		}
		registryWizard.finish();

		new WaitWhile(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_REGISTRY), TimePeriod.LONG);
		return getRegistry(registryName);
	}

	public OpenShiftODODevfileRegistry createNewRegistry(String registryName, String registryURL) {
		return createNewRegistry(registryName, registryURL, false);
	}

	/**
	 * Finds out whether a registry with specified registry name exists or not.
	 * 
	 * @param registryName registry name
	 * @return true if registry exists, false otherwise
	 */
	public boolean registryExists(String registryName) {
		try {
			return getRegistry(registryName) != null;
		} catch (RedDeerException ex) {
			return false;
		}
	}

}
