/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.OpenShiftEndpointException;

/**
 * @author André Dietisheim
 */
public class NewDomainWizard extends AbstractOpenShiftWizard<NewDomainWizardModel> {

	public NewDomainWizard(ExpressConnection connection) {
		super("Create Domain", new NewDomainWizardModel(connection));
	}

	@Override
	public boolean performFinish() {
		AbstractDelegatingMonitorJob newDomainJob = 
				new AbstractDelegatingMonitorJob(NLS.bind("Creating domain {0}...", getModel().getDomainId())) {
			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					getModel().createDomain();
					return Status.OK_STATUS;
				} catch (OpenShiftEndpointException e) {
					return ExpressUIActivator.createErrorStatus(
							NLS.bind("Could not create domain \"{0}\": {1}",
									getModel().getDomainId(), e.getRestResponseMessages()), e);
				}
			}
		};
		
		try {
			WizardUtils.runInWizard(newDomainJob, getContainer());
		} catch (Exception e) {
			Logger.error("Could not create domain", e);
		}
		return newDomainJob.getResult().isOK();
	}

	@Override
	public void addPages() {
		addPage(new NewDomainWizardPage(getModel(), this));
	}
}
