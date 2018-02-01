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
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * Job to delete a resource from a project/OpenShift namespace
 * 
 * @author jeff.cantrill
 */
public abstract class AbstractDeleteResourceJob extends AbstractDelegatingMonitorJob {

	public AbstractDeleteResourceJob(String name) {
		super(name);
	}

	protected IStatus delete(final IResource resource, final Connection connection, final IProgressMonitor monitor) {
		if (resource == null || monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		try {
			monitor.subTask(NLS.bind("Delete Resource {0}...", resource.getName()));
			if (connection != null) {
				connection.deleteResource(resource);
				ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection,
						ConnectionProperties.PROPERTY_RESOURCE, resource, null);
			}
			return Status.OK_STATUS;
		} catch (OpenShiftException e) {
			return new Status(Status.ERROR, OpenShiftCoreActivator.PLUGIN_ID,
					NLS.bind("Error deleting {0} named  {1}.", resource.getKind(), resource.getName()), e);
		}
	}

}
