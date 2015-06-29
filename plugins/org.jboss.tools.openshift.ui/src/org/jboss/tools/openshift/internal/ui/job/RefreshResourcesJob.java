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
package org.jboss.tools.openshift.internal.ui.job;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

/**
 * Job to refresh a list of resources from a project/OpenShift namespace
 * 
 * @author jeff.cantrill
 */
public class RefreshResourcesJob extends AbstractDelegatingMonitorJob {

	private IResourcesModel model;
	private boolean resourcesAdded;

	public RefreshResourcesJob(IResourcesModel model, boolean resourcesAdded) {
		super("Refresh Resources Job");
		this.model = model;
		this.resourcesAdded = true;
	}


	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Refreshing OpenShift resources...", IProgressMonitor.UNKNOWN);
			Collection<IResource> resources = model.getResources();
			if(resources == null || resources.isEmpty()) return Status.OK_STATUS;
			for (IResource resource : resources) {
				if(ResourceKind.STATUS.equals(resource.getKind())) {
					continue;
				}
				Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
				if(connection != null) {
					IResource newValue = ((Connection)connection).getResource(resource);
					IResource oldValue = resourcesAdded ? null : resource;
					ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection, ConnectionProperties.PROPERTY_RESOURCE, oldValue, newValue);
				}
			}
		}catch(Exception e) {
			return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Exception refreshing resources", e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
