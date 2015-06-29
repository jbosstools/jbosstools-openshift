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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;

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
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addPages() {
		addPage(new TemplateListPage(this, model));
		addPage(new TemplateParametersPage(this, model));
		addPage(new ResourceLabelsPage(this, model));
	}

	@Override
	public boolean performFinish() {

		final CreateApplicationFromTemplateJob createJob = new CreateApplicationFromTemplateJob(
				model.getProject(),
				model.getTemplate(),
				model.getParameters(),
				model.getLabels()
				);
		createJob.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if(JobUtils.isOk(status) || JobUtils.isWarning(status)) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							final String message = NLS.bind("Results of creating the resources from the {0} template.", model.getTemplate().getName());
							new ResourceSummaryDialog(getShell(), createJob.getResources(), "Create Application Summary",message, new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider()).open();
						}
					});
				}
			}
		});
		DelegatingProgressMonitor monitor = createJob.getDelegatingProgressMonitor();
		Job job = new JobChainBuilder(createJob)
				.runWhenDone(new RefreshResourcesJob(createJob, true)).build();

		try {
			IStatus status = runInWizard(
					job, 
					monitor, 
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
