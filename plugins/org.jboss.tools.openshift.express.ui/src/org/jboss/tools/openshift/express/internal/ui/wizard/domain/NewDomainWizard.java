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
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizard;

import com.openshift.client.OpenShiftEndpointException;

/**
 * @author André Dietisheim
 */
public class NewDomainWizard extends AbstractOpenShiftWizard<NewDomainWizardModel> {

	public NewDomainWizard(Connection connection) {
		super("Create Domain", new NewDomainWizardModel(connection));
	}

	@Override
	public boolean performFinish() {
		final boolean result[] = new boolean[]{false};
		try {
			WizardUtils.runInWizard(
					new Job(NLS.bind("Creating domain {0}...", getModel().getDomainId())) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								getModel().createDomain();
								result[0] = true;
							} catch (OpenShiftEndpointException e) {
								return OpenShiftUIActivator.createErrorStatus(
										NLS.bind(
												"Could not create domain \"{0}\": {1}", getModel().getDomainId(),
												e.getRestResponseMessages()), e);
							} catch (Exception e) {
								return OpenShiftUIActivator.createErrorStatus(NLS.bind(
										"Could not create domain \"{0}\"", getModel().getDomainId()), e);
							}
							return Status.OK_STATUS;
						}
			}, getContainer());
		} catch (Exception e) {
			Logger.error("Could not create domain", e);
		}
		return result[0];
	}
	
	@Override
	public void addPages() {
		addPage(new EditDomainWizardPage(
				"New OpenShift Domain", "Please provide a new name for your new OpenShift domain", getModel(), this));
	}
}
