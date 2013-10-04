/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ApplicationSelectionDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizard;

import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftException;

/**
 * @author Martes G Wigglesworth
 *
 */
public class ApplicationEnvironmentalVariableConfigurationWizardPage extends AbstractOpenShiftWizardPage {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPage
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPage(String title, String description, String pageName,
			IWizard wizard) {
		super(title, description, pageName, wizard);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage#doCreateControls(org.eclipse.swt.widgets.Composite, org.eclipse.core.databinding.DataBindingContext)
	 */
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		// TODO Auto-generated method stub

	}
	
	private SelectionListener onBrowseApps(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				OpenShiftApplicationWizard wizard = (OpenShiftApplicationWizard) getWizard();
				ApplicationEnvironmentalVariableConfigurationWizardPageModel wizardModel = wizard.getModel();
				final ApplicationSelectionDialog appSelectionDialog =
						new ApplicationSelectionDialog(wizard, wizardModel, null, getShell());
				final int result = appSelectionDialog.open();
				if (result == IDialogConstants.OK_ID) {
					final IApplication selectedApplication = appSelectionDialog.getSelectedApplication();
					if (selectedApplication != null) {
						// This setter may be long-running
						Job j = new Job("Setting Application") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									pageModel.setExistingApplicationName(selectedApplication.getName());
								} catch (OpenShiftException ex) {
									OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
											"Could not get embedded cartridges for application {0}",
											selectedApplication.getName()), ex));
								}
								return Status.OK_STATUS;
							}
						};
						try {
							WizardUtils.runInWizard(j, getContainer(), dbc);
						} catch (InvocationTargetException ite) {
						} catch (InterruptedException ie) {
						}
					}
				}
			}
		};
	}

}
