/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import static org.jboss.tools.common.ui.WizardUtils.runInWizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryContentProvider;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryLabelProvider;
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;

import com.openshift.restclient.model.IResource;

/**
 * The new application wizard that allows you to create an application given an
 * OpenShift template
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class NewApplicationWizard extends Wizard implements IWorkbenchWizard, IConnectionAwareWizard<Connection> {
	
	private NewApplicationWizardModel model;

	public NewApplicationWizard() {
		setWindowTitle("New OpenShift Application");
		setNeedsProgressMonitor(true);
		this.model = new NewApplicationWizardModel();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection.isEmpty()) {
			return;
		}
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if (connection != null) {
			model.setConnection(connection);
		} else {
			IResource resource = UIUtils.getFirstElement(selection, IResource.class);
			if (resource != null) {
				model.setConnection(ConnectionsRegistryUtil.safeGetConnectionFor(resource));
				model.setProject(resource.getProject());
			} else {
				ResourceGrouping grouping = UIUtils.getFirstElement(selection, ResourceGrouping.class);
				if (grouping != null) {
					model.setConnection(ConnectionsRegistryUtil.safeGetConnectionFor(grouping.getProject()));
					model.setProject(grouping.getProject());
				}
			}
		}
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
							final String message = NLS.bind(
									"Results of creating the resources from the {0} template.", 
									model.getTemplate().getName());
							new ResourceSummaryDialog(
									getShell(), 
									createJob.getResources(), 
									"Create Application Summary",
									message, 
									new ResourceSummaryLabelProvider(), new ResourceSummaryContentProvider()).open();
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
						createJob.getDelegatingProgressMonitor(), 
						getContainer());
				return isFailed(status);
			} catch (InvocationTargetException | InterruptedException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
				return false;
			}
	}

	private boolean isFailed(IStatus status) {
		return JobUtils.isOk(status) 
				|| JobUtils.isWarning(status);
	}

	@Override
	public Connection getConnection() {
		return model.getConnection();
	}

	@Override
	public boolean hasConnection() {
		return model.hasConnection();
	}

	@Override
	public Connection setConnection(Connection connection) {
		return model.setConnection(connection);
	}

	@Override
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
