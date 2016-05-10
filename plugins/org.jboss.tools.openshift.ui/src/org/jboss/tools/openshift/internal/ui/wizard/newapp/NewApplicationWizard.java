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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAwareWizard;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.job.IResourcesModelJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPage;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeploymentConfigPage;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.ServicesAndRoutingPage;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.ApplicationSourceFromImageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.BuildConfigPage;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.ApplicationSourceFromTemplateModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateParametersPage;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * The new application wizard that allows you to create an application given an
 * OpenShift template
 * 
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class NewApplicationWizard 
	extends Wizard 
	implements IWorkbenchWizard, IConnectionAwareWizard<Connection> {

	private NewApplicationWizardModel model = new NewApplicationWizardModel();
	private ApplicationSourceFromTemplateModel fromTemplateModel = new ApplicationSourceFromTemplateModel();
	private ApplicationSourceFromImageModel fromImageModel = new ApplicationSourceFromImageModel();

	public NewApplicationWizard() {
		setWindowTitle("New OpenShift Application");
		setNeedsProgressMonitor(true);
		
		Stream.of(fromTemplateModel, fromImageModel).forEach(m->{
			model.addPropertyChangeListener(IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE, m);
			model.addPropertyChangeListener(IApplicationSourceListPageModel.PROPERTY_ECLIPSE_PROJECT, m);
			model.addPropertyChangeListener(IApplicationSourceListPageModel.PROPERTY_PROJECT, m);
			model.addPropertyChangeListener(IResourceLabelsPageModel.PROPERTY_LABELS, m);
		});
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection == null
				|| selection.isEmpty()) {
			return;
		}
		fromImageModel.setContainer(getContainer());
		org.eclipse.core.resources.IProject selectedProject = UIUtils.getFirstElement(selection, org.eclipse.core.resources.IProject.class);
		model.setEclipseProject(selectedProject);
		
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if (connection != null) {
			setConnection(connection);
		} else {
			IResource resource = UIUtils.getFirstElement(selection, IResource.class);
			if (resource != null) {
				connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
				setConnection(connection);
				model.setProject(resource.getProject());
			} 
		}
		if(connection != null) {
			ConnectionsRegistrySingleton.getInstance().setRecent(connection);
		}
	}

	@Override
	public void addPages() {
		/*
		 * list --> template params -------------------------------> labels -> done
		 *   |                                                    |
		 *    ----> buildconfig -> deployconfig -> serviceconfig -|
		 */
		
		//app from image
		BuildConfigPage bcPage = new BuildConfigPage(this, fromImageModel) {
			@Override
			public boolean isPageComplete() {
				return isTemplateFlow() ? true : super.isPageComplete();
			}
		};

		DeploymentConfigPage dcPage = new DeploymentConfigPage(this, fromImageModel) {
			@Override
			public boolean isPageComplete() {
				return isTemplateFlow() ? true : super.isPageComplete();
			}
		};
		ServicesAndRoutingPage servicesPage = new ServicesAndRoutingPage(this, fromImageModel) {
			@Override
			public boolean isPageComplete() {
				return isTemplateFlow() ? true : super.isPageComplete();
			}
			
		};

		//app from template
		TemplateParametersPage paramPage = new TemplateParametersPage(this, fromTemplateModel) {
			
			@Override
			public boolean isPageComplete() {
				return isTemplateFlow() ? super.isPageComplete() : true;
			}

			@Override
			public IWizardPage getNextPage() {
				return getPage(ResourceLabelsPage.PAGE_NAME);
			}
		};
		
		ResourceLabelsPage labelsPage = new ResourceLabelsPage(this, model);

		ApplicationSourceListPage listPage = new ApplicationSourceListPage(this, model) {

			@Override
			public IWizardPage getNextPage() {
				if(model.getSelectedAppSource() == null) {
					return null;
				}
				if(isTemplateFlow()){
					Stream.of(bcPage, dcPage, servicesPage).forEach(p->p.setPageComplete(true));
					return getPage(TemplateParametersPage.PAGE_NAME);
				}
				return getPage(DeploymentConfigPage.PAGE_NAME);
			}
			
		};

		
		addPage(listPage);
		addPage(paramPage);
		addPage(bcPage);
		addPage(dcPage);
		addPage(servicesPage);
		addPage(labelsPage);
	}

	@Override
	public void dispose() {
		super.dispose();
		model.dispose();
		fromImageModel.dispose();
		fromTemplateModel.dispose();
		model = null;
		fromImageModel = null;
		fromTemplateModel = null;
	}

	private boolean isTemplateFlow() {
		if(model.getSelectedAppSource() != null && model.getSelectedAppSource().getSource() != null) {
			return ResourceKind.TEMPLATE.equals(model.getSelectedAppSource().getSource().getKind());
		}
		return true;
	}

	@Override
	public boolean performFinish() {

		final IResourcesModelJob createJob = isTemplateFlow()
				? fromTemplateModel.createFinishJob() 
				: fromImageModel.createFinishJob();
		
		createJob.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if(JobUtils.isOk(status) 
						|| JobUtils.isWarning(status)) {
					Display.getDefault().syncExec(createJob.getSummaryRunnable(getShell()));
					OpenShiftUIUtils.showOpenShiftExplorerView();
					if (model.getEclipseProject() != null) {
						//No need to import the project from git, it's already here
						return;
					}
					final Map<IProject, Collection<IBuildConfig>> projectsAndBuildConfigs = getBuildConfigs(createJob.getResources());
					if (projectsAndBuildConfigs.isEmpty()) {
						return;
					}
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							ImportApplicationWizard wizard = new ImportApplicationWizard(projectsAndBuildConfigs);
							new WizardDialog(getShell(), wizard).open();
						}
					});
				}
			}
			
			protected Map<IProject, Collection<IBuildConfig>> getBuildConfigs(Collection<IResource> resources) {
				Map<IProject, Collection<IBuildConfig>> projects = new LinkedHashMap<>();
				for (IResource resource : resources) {
					if (resource instanceof IBuildConfig) {
						IBuildConfig buildConfig = (IBuildConfig)resource;
						if (StringUtils.isNotBlank(buildConfig.getSourceURI())) {
							IProject p = buildConfig.getProject();
							Collection<IBuildConfig> buildConfigs = projects.get(p);
							if (buildConfigs == null) {
								buildConfigs = new LinkedHashSet<>();
								projects.put(p, buildConfigs);
							}
							buildConfigs.add(buildConfig);
						}
					}
				}
				return projects;
			}
		});
		boolean success = false;
		try {
			Job job = new JobChainBuilder(createJob.getJob())
					.runWhenSuccessfullyDone(new RefreshResourcesJob(createJob, true)).build();
			IStatus status = runInWizard(
					job, 
					createJob.getDelegatingProgressMonitor(), 
					getContainer());
			success = isFailed(status);
		} catch (InvocationTargetException | InterruptedException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
			success = false;
		} finally {
			UsageStats.getInstance().newV3Application(model.getConnection().getHost(), success);
		}
		return success;
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
	public void setConnection(Connection connection) {
		model.setConnection(connection);
		fromImageModel.setConnection(connection);
	}

	@Override
	public Object getContext() {
		return null;
	}
}
