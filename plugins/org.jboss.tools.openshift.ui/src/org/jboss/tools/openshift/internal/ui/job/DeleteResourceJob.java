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
package org.jboss.tools.openshift.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.utils.ResourceProcessor;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Job to delete a resource from a project/OpenShift namespace
 * 
 * @author jeff.cantrill
 * @author Jeff Maury
 */
public class DeleteResourceJob extends AbstractDelegatingMonitorJob {

	private IResource resource;
	private boolean willDeleteSubResources;

	public DeleteResourceJob(IResource resource, boolean willDeleteSubResources) {
		super("Delete Resource Job");
		this.resource = resource;
		this.willDeleteSubResources = willDeleteSubResources;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Delete Resource", IProgressMonitor.UNKNOWN);
			Connection connection = ConnectionsRegistryUtil.getConnectionFor(resource);
			ResourceProcessor processor = Platform.getAdapterManager().getAdapter(resource, ResourceProcessor.class);
			ConnectionsRegistry registry = ConnectionsRegistrySingleton.getInstance();
			if(connection != null) {
				processor.handleDelete(registry, connection, resource, willDeleteSubResources);
			}
			return Status.OK_STATUS;
		}catch(OpenShiftException e) {
			return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, NLS.bind("Error deleting {0} named  {1}.", resource.getKind(), resource.getName()), e);
		}finally {
			monitor.done();
		}
	}

}
