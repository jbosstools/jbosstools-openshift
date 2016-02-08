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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
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
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromTemplateJob;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.ResourceLabelsPage;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard;

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
public class NewApplicationWizard extends Wizard implements IWorkbenchWizard, IConnectionAwareWizard<Connection> {

	private NewApplicationWizardModel model;

	public NewApplicationWizard() {
		setWindowTitle("New OpenShift Application");
		setNeedsProgressMonitor(true);
		this.model = new NewApplicationWizardModel();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if (selection == null
				|| selection.isEmpty()) {
			return;
		}
		org.eclipse.core.resources.IProject selectedProject = UIUtils.getFirstElement(selection, org.eclipse.core.resources.IProject.class);
		model.setEclipseProject(selectedProject);
		
		Connection connection = UIUtils.getFirstElement(selection, Connection.class);
		if (connection != null) {
			model.setConnection(connection);
		} else {
			IResource resource = UIUtils.getFirstElement(selection, IResource.class);
			if (resource != null) {
				connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
				model.setConnection(connection);
				model.setProject(resource.getProject());
			} 
		}
		if(connection != null) {
			ConnectionsRegistrySingleton.getInstance().setRecent(connection);
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
				model.getSelectedTemplate(),
				model.getParameters(),
				model.getLabels());
		
		createJob.addJobChangeListener(new JobChangeAdapter(){

			@Override
			public void done(IJobChangeEvent event) {
				IStatus status = event.getResult();
				if(JobUtils.isOk(status) 
						|| JobUtils.isWarning(status)) {
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							final String message = NLS.bind(
									"Results of creating the resources from the {0} template.", 
									model.getSelectedTemplate().getName());
							new NewApplicationSummaryDialog(getShell(), createJob, message).open();
						}
					});
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
			Job job = new JobChainBuilder(createJob)
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
	}

	@Override
	public Object getContext() {
		// TODO Auto-generated method stub
		return null;
	}
}
