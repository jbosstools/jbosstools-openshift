/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import static org.jboss.tools.common.ui.WizardUtils.runInWizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.job.DeployImageJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPage;


/**
 * The deploy image wizard that allows you to deploy a docker image to openshift
 * with a reasonable set of openshift resources
 * OpenShift template
 * 
 * @author jeff.cantrill
 */
public class DeployImageWizard extends AbstractOpenShiftWizard<IDeployImageParameters> {

	private static final String TITLE = "Deploy Image to OpenShift";

	public DeployImageWizard(IDeployImageParameters wizardModel) {
		super(TITLE, wizardModel);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(new DeployImagePage(this, getModel()));
		addPage(new DeploymentConfigPage(this, getModel()));
		addPage(new ServicesAndRoutingPage(this,  getModel()));
		addPage(new ResourceLabelsPage(this,  getModel()));
	}

	@Override
	public boolean performFinish() {
		final DeployImageJob deployJob = new DeployImageJob( getModel());
		deployJob.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if(JobUtils.isOk(status) || JobUtils.isWarning(status)) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							final String message = NLS.bind(
									"Results of deploying image \"{0}\".",  getModel().getResourceName());
							new ResourceSummaryDialog(
									getShell(), 
									deployJob.getResources(),
									TITLE,
									message).open();
						}
					});
					OpenShiftUIUtils.showOpenShiftExplorerView();
				}
			}
		});
		boolean success = false;
		try {
			Job job = new JobChainBuilder(deployJob)
					.runWhenSuccessfullyDone(new RefreshResourcesJob(deployJob, true))
					.build();
			IStatus status = runInWizard(
					job, 
					deployJob.getDelegatingProgressMonitor(), 
					getContainer());
			success = isFailed(status);
		} catch (InvocationTargetException | InterruptedException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
			success = false;
		} finally {
			UsageStats.getInstance().newV3Application( getModel().getConnection().getHost(), success);
		}
		getModel().release();
		return success;
	}
	
	public static boolean isFailed(IStatus status) {
		return JobUtils.isOk(status) || JobUtils.isWarning(status);
	}

}
