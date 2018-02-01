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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.core.WatchManager;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.http.IHttpConstants;
import com.openshift.restclient.model.IProject;

public class DeleteProjectJob extends DeleteResourceJob {

	private static final long PROJECT_DELETE_DELAY = 500;
	private static final long MAX_PROJECT_DELETE_DELAY = 5000;

	DeleteProjectJob(IProject project) {
		super(project);
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		final IProject project = (IProject) resource;
		Connection connection = ConnectionsRegistryUtil.getConnectionFor(project);
		WatchManager.getInstance().stopWatch(project, connection);
		List<IProject> oldProjects = connection.getResources(ResourceKind.PROJECT);
		IStatus status = super.doRun(monitor);
		if (status.isOK() && waitForServerToReconcileProjectDelete(connection, project)) {
			List<IProject> newProjects = new ArrayList<>(oldProjects);
			newProjects.remove(project);
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection,
					ConnectionProperties.PROPERTY_PROJECTS, oldProjects, newProjects);
		}
		return status;
	}

	private boolean waitForServerToReconcileProjectDelete(final Connection conn, final IProject project) {
		boolean deleted = false;
		long sleep = 0;
		do {
			try {
				conn.refresh(project);
				Thread.sleep(PROJECT_DELETE_DELAY);
			} catch (InterruptedException ie) {
			} catch (OpenShiftException ex) {
				if (ex.getStatus() != null) {
					final int code = ex.getStatus().getCode();
					if (code == IHttpConstants.STATUS_NOT_FOUND || code == IHttpConstants.STATUS_FORBIDDEN) {
						deleted = true;
					}
				}
			} finally {
				sleep = sleep + PROJECT_DELETE_DELAY;
			}
		} while (!deleted && sleep < MAX_PROJECT_DELETE_DELAY);
		return deleted;
	}
}
