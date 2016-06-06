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
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageSearchResult;
import org.eclipse.linuxtools.docker.core.IRegistryAccount;
import org.eclipse.linuxtools.docker.core.IRepositoryTag;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.foundation.core.jobs.DelegatingProgressMonitor;
import org.jboss.tools.openshift.common.ui.wizard.AbstractOpenShiftWizard;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPage;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.dockerutils.PushImageToRegistryJob;
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
		if(connection != null) {
			model.setTargetRegistryLocation(
				(String) connection.getExtendedProperties().get(ICommonAttributes.IMAGE_REGISTRY_URL_KEY));
			model.setTargetRegistryUsername(connection.getUsername());
			model.setTargetRegistryPassword(connection.getToken());
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
		// checks if we need to push the image, first
		final Job job = getJobChain(getModel(), PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				final IStatus status = event.getResult();
				UsageStats.getInstance().newV3Application( getModel().getConnection().getHost(), isFailed(status));
				super.done(event);
			}
		});
		job.schedule();
			
		return true;
	}

	/**
	 * Gets the Job to run as a chain of smaller jobs, depending on the use-case
	 * @param model the wizard model
	 * @param shell the current shell
	 * @return
	 */
	private Job getJobChain(final IDeployImageParameters model, final Shell shell) {
		final DeployImageJob deployJob = getDeployImageJob(getModel(), getShell());
		final boolean pushImageToRegistry = model.isPushImageToRegistry();
		if(pushImageToRegistry) {
			final PushImageToRegistryJob pushImageToRegistryJob = getPushImageToRegistryJob(model);
			return new JobChainBuilder(pushImageToRegistryJob).runWhenSuccessfullyDone(deployJob)
					.runWhenSuccessfullyDone(new RefreshResourcesJob(deployJob, true)).build();
		}
		return new JobChainBuilder(deployJob).runWhenSuccessfullyDone(new RefreshResourcesJob(deployJob, true)).build();
	}

	private static DeployImageJob getDeployImageJob(final IDeployImageParameters model, final Shell shell) {
		final DeployImageJob deployJob = new DeployImageJob(model);
		deployJob.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if((JobUtils.isOk(status) || JobUtils.isWarning(status)) && !deployJob.getResources().isEmpty()) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							new ResourceSummaryDialog(
									shell, 
									deployJob.getResources(),
									TITLE,
									deployJob.getSummaryMessage()).open();
						}
					});
					OpenShiftUIUtils.showOpenShiftExplorer();
				}
			}
		});
		return deployJob;
	}
	
	private static PushImageToRegistryJob getPushImageToRegistryJob(final IDeployImageParameters model) {
		final IDockerConnection dockerConnection = model.getDockerConnection();
		final String imageName = model.getImageName();
		final String deployProjectName = model.getProject().getName();
		final IRegistryAccount registryAccount = new IRegistryAccount() {
			
			@Override
			public String getServerAddress() {
				return model.getTargetRegistryLocation();
			}
			
			@Override
			public String getUsername() {
				return model.getTargetRegistryUsername();
			}
			
			@Override
			public char[] getPassword() {
				return model.getTargetRegistryPassword().toCharArray();
			}
			
			@Override
			public String getEmail() {
				return null;
			}
			
			@Override
			public List<IRepositoryTag> getTags(String arg0) throws DockerException {
				return null;
			}
			
			@Override
			public boolean isVersion2() {
				return false;
			}

			@Override
			public List<IDockerImageSearchResult> getImages(String arg0) throws DockerException {
				return null;
			}
		}; 
		return new PushImageToRegistryJob(dockerConnection, registryAccount, deployProjectName, imageName);
	}
	
	/**
	 * Checks if the given {@code status}
	 * 
	 * @param status
	 *            the {@link IStatus} to check
	 * @return <code>true</code> if the given status severity is
	 *         {@link IStatus.OK} or {@link IStatus.WARNING}, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isFailed(IStatus status) {
		return JobUtils.isOk(status) || JobUtils.isWarning(status);
	}

    @Override
    public void dispose() {
        super.dispose();
        //getModel().dispose();
    }

}
