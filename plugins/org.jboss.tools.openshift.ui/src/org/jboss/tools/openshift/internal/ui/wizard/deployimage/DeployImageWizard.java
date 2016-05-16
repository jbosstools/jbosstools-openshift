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
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPage;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.job.DeployImageJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPage;

import com.openshift.restclient.model.IProject;


/**
 * The deploy image wizard that allows you to deploy a docker image to openshift
 * with a reasonable set of openshift resources
 * OpenShift template
 * 
 * @author jeff.cantrill
 */
public class DeployImageWizard extends AbstractOpenShiftWizard<IDeployImageParameters> {

	private static final String TITLE = "Deploy Image to OpenShift";

	public DeployImageWizard(IDockerImage image, Connection connection, IProject project, boolean isConnected) {
		super(TITLE, new DeployImageWizardModel());

		DeployImageWizardModel model = (DeployImageWizardModel)getModel();
		if(image != null) {
			IDockerConnection dockerConnection = image.getConnection();
			model.setOriginatedFromDockerExplorer(true);
			model.setDockerConnection(dockerConnection);
			model.setImageName(image.repo());
		}
		if(project != null) {
			model.initModel(ConnectionsRegistryUtil.getConnectionFor(project), project);
		} else {
			if(connection != null) {
				model.setConnection(connection);
			}
		}

		model.setStartedWithActiveConnection(isConnected);

		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
	    if (getModel().originatedFromDockerExplorer()
				|| !((DeployImageWizardModel)getModel()).isStartedWithActiveConnection()) {
	        addPage(new ConnectionWizardPage(this, getModel(), Connection.class));
	    }
		addPage(new DeployImagePage(this, getModel()));
		addPage(new DeploymentConfigPage(this, getModel()));
		addPage(new ServicesAndRoutingPage(this,  getModel()));
		addPage(new ResourceLabelsPage(this,  getModel()));
	}

	@Override
	public IWizardPage getStartingPage() {
		if(((DeployImageWizardModel)getModel()).isStartedWithActiveConnection()) {
			return getPage(DeployImagePage.DEPLOY_IMAGE_PAGE_NAME);
		}
		return super.getStartingPage();
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
							new ResourceSummaryDialog(
									getShell(), 
									deployJob.getResources(),
									TITLE,
									deployJob.getSummaryMessage()).open();
						}
					});
					OpenShiftUIUtils.showOpenShiftExplorer();
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
		return success;
	}
	
	public static boolean isFailed(IStatus status) {
		return JobUtils.isOk(status) || JobUtils.isWarning(status);
	}

    @Override
    public void dispose() {
        super.dispose();
        getModel().dispose();
    }

}
