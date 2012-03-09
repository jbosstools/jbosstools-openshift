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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.OpenShiftEndpointException;

/**
 * @author André Dietisheim
 */
public class NewDomainDialog extends Wizard {

	private NewDomainWizardPageModel model;

	public NewDomainDialog(UserDelegate user) {
		this.model = new NewDomainWizardPageModel(user);
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		try {
			WizardUtils.runInWizard(new Job("Creating domain...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.createDomain();
					} catch (OpenShiftEndpointException e) {
						return OpenShiftUIActivator.createErrorStatus(NLS.bind(
								"Could not create domain \"{0}\": {1}", model.getNamespace(), e.getResponseResult()), e);
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus(NLS.bind(
								"Could not create domain \"{0}\"", model.getNamespace()), e);
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
		} catch (Exception e) {
		}
		return true;
	}

	@Override
	public void addPages() {
		addPage(new NewDomainWizardPage(model, this));
	}
}
