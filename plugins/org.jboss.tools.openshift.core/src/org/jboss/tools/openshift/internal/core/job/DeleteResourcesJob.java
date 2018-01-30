/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.job;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;

import com.openshift.restclient.model.IResource;

/**
 * Deletes the given list of resources.
 * 
 * @author Andre Dietisheim
 */
public class DeleteResourcesJob extends AbstractDeleteResourceJob {

	private List<IResource> resources;

	public DeleteResourcesJob(final List<IResource> resources) {
		super("Delete Resource Job");
		this.resources = resources;
	}

	@Override
	protected IStatus doRun(final IProgressMonitor monitor) {
		if (CollectionUtils.isEmpty(resources)) {
			return Status.CANCEL_STATUS;
		}
		List<IStatus> status = resources.stream().map(resource -> {
			return delete(resource, ConnectionsRegistryUtil.getConnectionFor(resource), monitor);
		}).filter(s -> s.getSeverity() == IStatus.ERROR).collect(Collectors.toList());

		if (CollectionUtils.isEmpty(status)) {
			return Status.OK_STATUS;
		}
		return new MultiStatus(OpenShiftCommonCoreActivator.PLUGIN_ID, 0, status.toArray(new IStatus[status.size()]),
				"Could not remove certain resources", null);
	}

}
