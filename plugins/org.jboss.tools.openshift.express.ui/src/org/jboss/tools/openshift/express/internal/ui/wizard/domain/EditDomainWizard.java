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
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftEndpointException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizard extends AbstractOpenShiftWizard<DomainWizardModel> {

	public EditDomainWizard(IDomain domain) {
		super("Edit domain", new DomainWizardModel(domain));
	}

	@Override
	public boolean performFinish() {
		AbstractDelegatingMonitorJob job = new AbstractDelegatingMonitorJob("Renaming domain...") {
			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				try {
					getModel().renameDomain();
					return Status.OK_STATUS;
				} catch (OpenShiftEndpointException e) {
					return ExpressUIActivator.createErrorStatus(
							NLS.bind(
									"Could not rename domain \"{0}\": {1}", getModel().getDomainId(),
									e.getRestResponseMessages()), e);
				} catch (Exception e) {
					return ExpressUIActivator.createErrorStatus(NLS.bind(
							"Could not rename domain {0}", getModel().getDomainId()), e);
				}
			}
		};
		try {
			IStatus status = WizardUtils.runInWizard(job, job.getDelegatingProgressMonitor(), getContainer());
			return status.isOK();
		} catch (Exception ex) {
			Logger.error("Could not rename domain", ex);
			return false;
		}
	}

	@Override
	public void addPages() {
		addPage(new EditDomainWizardPage(getModel(), this));
	}
}
