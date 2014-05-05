/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.job.FireConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftResource;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class RefreshResourceHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		Object resource = getResource(selection);
		if (resource != null) {
			refresh(resource);
		}
		return null;
	}

	private Object getResource(ISelection selection) {
		Object resource = UIUtils.getFirstElement(selection, IOpenShiftResource.class);
		if (resource == null) {
			resource = UIUtils.getFirstElement(selection, Connection.class);
		}
		return resource;
	}

	private void refresh(final Object element) {
		Job job = new AbstractDelegatingMonitorJob("Loading OpenShift information...") {

			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Loading OpenShift informations...", IProgressMonitor.UNKNOWN);
					if(element instanceof Connection) {
						((Connection)element).refresh();
					} else if (element instanceof IOpenShiftResource) {
						((IOpenShiftResource)element).refresh();
					}
				} catch (OpenShiftException e) {
					Logger.error("Failed to refresh element", e);
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		Connection connection = getConnection(element);
		if (connection != null) {
			new JobChainBuilder(job)
			.runWhenSuccessfullyDone(new FireConnectionsChangedJob(connection)).schedule();;
		} else {
			job.schedule();
		}
	}

	private Connection getConnection(final Object resource) {
		Connection connection = null;
		if (resource instanceof Connection) {
			connection = (Connection) resource; 
		} else if (resource instanceof IDomain) {
			IDomain domain = (IDomain) resource;
			connection = ConnectionsModelSingleton.getInstance().getConnectionByResource(domain.getUser());
		} else if (resource instanceof IApplication) {
			IApplication application = (IApplication) resource;
			connection = ConnectionsModelSingleton.getInstance().getConnectionByResource(application);
		}
		return connection;
	}
}
