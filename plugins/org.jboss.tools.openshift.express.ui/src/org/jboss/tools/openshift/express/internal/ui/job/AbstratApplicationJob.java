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
package org.jboss.tools.openshift.express.internal.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstratApplicationJob extends AbstractDelegatingMonitorJob {

	private ApplicationProvider applicationProvider;

	protected AbstratApplicationJob(ApplicationProvider applicationProvider, String name) {
		super(name);
		this.applicationProvider = applicationProvider;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		Logger.debug(getName());
		IApplication application = null;
		try {
			application = applicationProvider.getApplication();
			if (application != null) {
				doRun(application);
			}
		} catch (OpenShiftException e) {
			return OpenShiftUIActivator.createErrorStatus(NLS.bind("Could not {0}", getName()), e);
		}
		return Status.OK_STATUS;
	}

	protected abstract void doRun(IApplication application);
	
	public static class ApplicationProvider {
		
		private IApplication application;
		private LoadApplicationJob applicationJob;
		
		public ApplicationProvider(IApplication application) {
			this.application = application;
		}

		public ApplicationProvider(LoadApplicationJob applicationJob) {
			this.applicationJob = applicationJob;
		}

		public IApplication getApplication() {
			if (application != null) {
				return application;
			} else {
				return applicationJob.getApplication();
			}
		}
	}
}
