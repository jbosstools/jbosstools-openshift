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
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;

import io.fabric8.servicecatalog.api.model.ServiceInstance;

/**
 * @author Red Hat Developers
 *
 */
public class ServiceElement extends AbstractOpenshiftUIElement<ServiceInstance, ApplicationElement, ApplicationExplorerUIModel> {
	
	public ServiceElement(ServiceInstance service, ApplicationElement parentElement) {
		super(parentElement, service);
	}

}
