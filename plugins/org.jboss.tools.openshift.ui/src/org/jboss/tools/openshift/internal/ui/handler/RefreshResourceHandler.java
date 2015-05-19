/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.common.core.IRefreshable;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * @author Jeff Cantrill
 */
public class RefreshResourceHandler extends AbstractHandler{

	private static final String FAILED_TO_REFRESH_ELEMENT = "Failed to refresh element";
	private static final String LOADING_OPEN_SHIFT_INFORMATIONS = "Loading OpenShift information...";

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
		Object resource = UIUtils.getFirstElement(selection, IRefreshable.class);
		if (resource == null) {
			resource = UIUtils.getFirstElement(selection, IConnection.class);
		}
		return resource;
	}

	private void refresh(final Object element) {
			Job job = new AbstractDelegatingMonitorJob(LOADING_OPEN_SHIFT_INFORMATIONS) {
	
				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						monitor.beginTask(LOADING_OPEN_SHIFT_INFORMATIONS, IProgressMonitor.UNKNOWN);
						if (element instanceof IRefreshable) {
							((IRefreshable) element).refresh();
						} 
					} catch (OpenShiftException e) {
						OpenShiftCommonUIActivator.getDefault().getLogger().logError(FAILED_TO_REFRESH_ELEMENT, e);
						return new Status(Status.ERROR, OpenShiftCommonUIActivator.PLUGIN_ID, FAILED_TO_REFRESH_ELEMENT, e);
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
	
			IConnection connection = getConnection(element);
			if (connection != null) {
				new JobChainBuilder(job)
				.runWhenSuccessfullyDone(new org.jboss.tools.openshift.internal.common.core.job.FireConnectionsChangedJob(connection)).schedule();
			} else {
				job.schedule();
			}
	}
	
	private IConnection getConnection(final Object resource) {
		if (resource instanceof IConnection) {
			return (IConnection) resource; 
		}else if(resource instanceof IResource) {
			return ConnectionsRegistryUtil.getConnectionFor((IResource)resource);
		}
		return null;
	}
}
