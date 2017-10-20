/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import org.jboss.tools.common.databinding.ObservablePojo;

import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IReplicationController;

public class EditResourceLimitsPageModel extends ObservablePojo {

	public static final String SELECTED_CONTAINER = "selectedContainer";
	public static final String CONTAINERS = "containers";

	public static final String REQUESTS_MEMORY = "requestsMemory";
	public static final String REQUESTS_CPU = "requestsCPU";
	public static final String LIMITS_MEMORY = "limitsMemory";
	public static final String LIMITS_CPU = "limitsCPU";

	private IReplicationController rc;
	private IContainer selectedContainer;

	public EditResourceLimitsPageModel(IReplicationController rc) {
		this.rc = rc.getCapability(IClientCapability.class).getClient().getResourceFactory().create(rc.toJson(true));
		setSelectedContainer(this.rc.getContainers().iterator().next());
	}

	public IReplicationController getUpdatedReplicationController() {
		return rc;
	}

	public void setSelectedContainer(IContainer selectedContainer) {
		firePropertyChange(SELECTED_CONTAINER, this.selectedContainer, this.selectedContainer = selectedContainer);
	}

	public IContainer getSelectedContainer() {
		return selectedContainer;
	}
}