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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.RefreshConnectionsModelJob;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftEndpointException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainWizard extends AbstractOpenShiftWizard<EditDomainWizardModel> {

	public EditDomainWizard(IDomain domain) {
		super("Edit domain", new EditDomainWizardModel(domain));
	}

	@Override
	public boolean performFinish() {
		final boolean result[] = new boolean[] { false };
		Job job = new JobChainBuilder(new Job("Renaming domain...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getModel().renameDomain();
					result[0] = true;
					return Status.OK_STATUS;
				} catch (OpenShiftEndpointException e) {
					return OpenShiftUIActivator.createErrorStatus(
							NLS.bind(
									"Could not rename domain \"{0}\": {1}", getModel().getDomainId(),
									e.getRestResponseMessages()), e);
				} catch (Exception e) {
					return OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not rename domain {0}", getModel().getDomainId()), e);
				}
			}
		})
				.andRunWhenSuccessfull(
						new RefreshConnectionsModelJob(getModel().getDomain().getUser()))
				.build();
		try {
			WizardUtils.runInWizard(job, getContainer());
		} catch (Exception ex) {
			Logger.error("Could not rename domain", ex);
		}
		return result[0];
	}

	@Override
	public void addPages() {
		addPage(new EditDomainWizardPage(
				"OpenShift Domain Name", "Please provide a new name for your OpenShift domain", getModel(), this));
	}
}
