/*******************************************************************************
 * Copyright (c) 2015-2019 Red Hat, Inc.
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IResource;

/**
 * @author Fred Bricon
 * @author Jeff Maury
 */
public class OpenInWebConsoleHandler extends AbstractHandler {

	private String consoleURL;

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = UIUtils.getCurrentSelection(event);
		IResource resource = UIUtils.getFirstElement(currentSelection, IResource.class);
		Connection connection = getConnection(currentSelection, resource);

		return loadConsoleUrl(connection, resource);
	}

	protected <R extends IResource> IStatus loadConsoleUrl(Connection connection, R resource) {
		if (connection == null) {
			return new Status(IStatus.WARNING, OpenShiftUIActivator.PLUGIN_ID, 
					"Could not find an OpenShift connection to open a console for.");
		}
		
		new JobChainBuilder(Job.create(NLS.bind("Retrieving console for {0}...", connection.getHost()), 
				monitor -> {
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					setConsoleURL(connection.getConsoleURL(resource, monitor));
					if (StringUtils.isEmpty(consoleURL)) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								NLS.bind("Could not determine the url for the web console on {0}", connection.getHost()));
					}
					return Status.OK_STATUS;
				}))
		.runWhenSuccessfullyDone(new UIJob(NLS.bind("Opening console for {0}...", connection.getHost())) {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				new BrowserUtility().checkedCreateExternalBrowser(getConsoleURL(), OpenShiftUIActivator.PLUGIN_ID,
						OpenShiftUIActivator.getDefault().getLog());
				return Status.OK_STATUS;
			}
		})
		.schedule();
		return Status.OK_STATUS;
	}

	protected synchronized String getConsoleURL() {
		return consoleURL;
	}

	protected synchronized void setConsoleURL(String consoleURL) {
		this.consoleURL = consoleURL;
	}

	private Connection getConnection(ISelection currentSelection, IResource resource) {
		Connection connection = null;
		if (resource == null) {
			connection = UIUtils.getFirstElement(currentSelection, Connection.class);
		} else {
			connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
		}
		return connection;
	}

}