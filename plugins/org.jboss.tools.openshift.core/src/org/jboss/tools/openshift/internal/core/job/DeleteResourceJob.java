/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;

import com.openshift.restclient.model.IResource;

/**
 * Job to delete a resource from a project/OpenShift namespace
 * 
 * @author jeff.cantrill
 */
public class DeleteResourceJob extends AbstractDeleteResourceJob {

	protected IResource resource;

	public DeleteResourceJob(IResource resource) {
		super("Delete Resource Job");
		this.resource = resource;
	}

	@Override
	protected IStatus doRun(final IProgressMonitor monitor) {
		return delete(resource, ConnectionsRegistryUtil.getConnectionFor(resource), monitor);
	}
}
