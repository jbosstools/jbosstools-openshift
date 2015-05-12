/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.application;

import static org.jboss.tools.common.ui.WizardUtils.runInWizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;

/**
 * The new application wizard that allows you to create an application given an
 * OpenShift template
 * 
 * @author jeff.cantrill
 */
public class NewApplicationWizard extends Wizard implements IConnectionAwareWizard<Connection> {
	
	private NewApplicationWizardModel model;

	public NewApplicationWizard() {
		this(null);
	}
	
	public NewApplicationWizard(NewApplicationWizardModel model) {
		setWindowTitle("New OpenShift Application");
		this.model = model;
	}

	@Override
	public void addPages() {
		addPage(new TemplateListPage(this, model));
		addPage(new TemplateParametersPage(this, model));
		addPage(new ResourceLabelsPage(this, model));
	}

	@Override
	public boolean performFinish() {

		final CreateApplicationFromTemplateJob job = new CreateApplicationFromTemplateJob(
				model.getProject(),
				model.getTemplate(),
				model.getParameters(),
				model.getLabels()
				);
		job.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if(JobUtils.isOk(status) || JobUtils.isWarning(status)) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							final String message = NLS.bind("Results of creating the resources from the {0} template.", model.getTemplate().getName());
							new ResourceSummaryDialog(getShell(), job.getResources(), "Create Application Summary",message, new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider()).open();
						}
					});
				}
			}
		});

		try {
			IStatus status = runInWizard(
					job, 
					job.getDelegatingProgressMonitor(), 
					getContainer());
			return handleJobStatus(status);
		} catch (Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
			return handleJobStatus(new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID,"Error trying to create a new application from a template",e));
		}
	}

	private boolean handleJobStatus(IStatus status) {
		if (!JobUtils.isOk(status) && !JobUtils.isWarning(status)) {
			return false;
		}
		return true;
	}

	@Override
	public Connection getConnection() {
		// TODO implement so that it can react to connection changes
		return null;
	}

	@Override
	public boolean hasConnection() {
		// TODO implement so that it can react to connection changes
		return false;
	}

	@Override
	public Connection setConnection(Connection connection) {
		// TODO implement so that it can react to connection changes
		return null;
	}
}
