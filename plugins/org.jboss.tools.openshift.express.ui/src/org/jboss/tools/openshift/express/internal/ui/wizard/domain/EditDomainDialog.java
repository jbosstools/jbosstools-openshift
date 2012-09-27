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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.OpenShiftEndpointException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class EditDomainDialog extends Wizard {

	private EditDomainWizardPageModel model;

	public EditDomainDialog(UserDelegate user) {
		this.model = new EditDomainWizardPageModel(user);
		setNeedsProgressMonitor(true);
		setWindowTitle("Edit Domain");
	}

	@Override
	public boolean performFinish() {
		final boolean result[] = new boolean[]{false};
		try {
			WizardUtils.runInWizard(new Job("Renaming domain...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.renameDomain();
						result[0] = true;
						return Status.OK_STATUS;
					} catch(OpenShiftEndpointException e) {
						return OpenShiftUIActivator.createErrorStatus(NLS.bind(
								"Could not rename domain \"{0}\": {1}", model.getDomainId(), e.getRestResponseMessages()), e);
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus(NLS.bind(
								"Could not rename domain {0}", model.getDomainId()), e);
					}
				}
			}, getContainer());
		} catch (Exception ex) {
			Logger.error("Could not rename domain", ex);
		}
		return result[0];
	}

	@Override
	public void addPages() {
		addPage(new EditDomainWizardPage(model, this));
	}
}
