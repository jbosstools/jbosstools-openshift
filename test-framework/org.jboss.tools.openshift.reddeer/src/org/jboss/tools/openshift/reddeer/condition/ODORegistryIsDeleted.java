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
package org.jboss.tools.openshift.reddeer.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODODevfileRegistries;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODODevfileRegistry;

/**
 * Wait condition to wait for registry is deleted.
 * 
 */
public class ODORegistryIsDeleted extends AbstractWaitCondition {
	
	private String registryName;

	/**
	 * Constructs ODORegistryIsDeleted wait condition. Condition is met when project is deleted.
	 * 
	 * @param registryName project name
	 */
	public ODORegistryIsDeleted(String registryName) {
		this.registryName = registryName;
	}
	
	@Override
	public boolean test() {
		try {
			OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
			explorer.open();
			OpenShiftODODevfileRegistries registries = explorer.getOpenShiftODORegistries();
			OpenShiftODODevfileRegistry registry = registries.getRegistry(registryName);
			return registry == null;
		} catch (RedDeerException ex) {
			return false;
		}
	}

	@Override
	public String description() {
		return "ODO registry with name:"+ registryName +" is deleted";
	}
}
