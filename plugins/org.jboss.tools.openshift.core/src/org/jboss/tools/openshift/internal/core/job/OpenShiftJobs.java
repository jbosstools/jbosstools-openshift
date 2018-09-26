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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.jobs.Job;

import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * Utility class to handle OpenShift {@link Job}s
 * 
 * @author Fred Bricon
 * @author Jeff Maury
 */
public class OpenShiftJobs {

	private OpenShiftJobs() {
	}

	/**
	 * Creates a {@link DeleteResourceJob} to delete an OpenShift {@link IProject}.
	 */
	public static DeleteResourceJob createDeleteProjectJob(final IProject project) {
		Assert.isNotNull(project, "A project must not be null");

		return new DeleteProjectJob(project);
	}

	/**
	 * Creates a {@link DeleteResourceJob} to delete an OpenShift {@link IResource}.
	 */
	public static DeleteResourceJob createDeleteResourceJob(final IResource resource) {
		if (resource instanceof IProject) {
			return createDeleteProjectJob((IProject) resource);
		} else {
			return new DeleteResourceJob(resource);
		}
	}
}
