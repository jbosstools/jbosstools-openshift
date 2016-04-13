/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.core.IDockerImageMetadata;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromImageJob;
import org.jboss.tools.openshift.internal.ui.job.IResourcesModelJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariablesPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.IEnvironmentVariablesPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizardModel;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.ImageStreamTagMetaData;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSourceListPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSourceModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * A Model to handle creating a microservice application
 * based on an image
 * 
 * @author jeff.cantrill
 *
 */
public class ApplicationSourceFromImageModel 
	extends DeployImageWizardModel	
	implements IApplicationSourceModel, IBuildConfigPageModel {
	
	private static final String ANNOTATION_SAMPLE_CONTEXT_DIR = "sampleContextDir";
	private static final String ANNOTATION_SAMPLE_REPO = "sampleRepo";
	private static final String ANNOTATION_SAMPLE_REF = "sampleRef";
	private static final String DEFAULT_REFERENCE = "master";

	private ImageStreamApplicationSource source;
	private IWizardContainer container;
	private IEnvironmentVariablesPageModel envModel = new EnvironmentVariablesPageModel();
	private String gitRepositoryUrl = "";
	private String gitReference = DEFAULT_REFERENCE;
	private String contextDir = "";
	private boolean imageChangeTrigger = true;
	private boolean configChangeTrigger = true;
	private boolean configWebHook = true;
	private AtomicBoolean staleRepoInfo = new AtomicBoolean(true);
	
	@Override
	public IResourcesModelJob createFinishJob() {
		return new CreateApplicationFromImageJob(this, this);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt == null) return;
		super.propertyChange(evt);
		switch(evt.getPropertyName()) {
		case IResourceLabelsPageModel.PROPERTY_LABELS:
			setLabels((List<Label>) evt.getNewValue());
			break;
		case IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE:
			handleSelectedAppSource(evt);
			break;
		case IApplicationSourceListPageModel.PROPERTY_PROJECT:
			setProject((IProject) evt.getNewValue());
			break;
		case IApplicationSourceListPageModel.PROPERTY_ECLIPSE_PROJECT:
			handleEclipseProject(evt);
			break;
		}
	}
	
	private void handleEclipseProject(PropertyChangeEvent evt) {
		org.eclipse.core.resources.IProject project = (org.eclipse.core.resources.IProject) evt.getNewValue();
		if(project != null) {
			try {
				setGitRepositoryUrl(EGitUtils.getDefaultRemoteRepo(project));
				return;
			} catch (CoreException e) {
				OpenShiftUIActivator.getDefault().getLogger().logWarning("Unable to retrieve the remote git repo from " + project.getName(), e);
			}
		}
		setGitRepositoryUrl(null);
	}

	private void handleSelectedAppSource(PropertyChangeEvent evt) {
		if(evt.getNewValue() instanceof ImageStreamApplicationSource
				&& ResourceKind.IMAGE_STREAM.equals(((IApplicationSource) evt.getNewValue()).getKind())) {
			this.source = (ImageStreamApplicationSource) evt.getNewValue();
			staleRepoInfo.set(true);
		}
	}
	
	public void init() { 
		if(staleRepoInfo.compareAndSet(true, false)) {
			loadBuilderImageMetadata();
			if(this.source != null) {
				setGitRepositoryUrl(this.source.getAnnotation(ANNOTATION_SAMPLE_REPO));
				setContextDir(this.source.getAnnotation(ANNOTATION_SAMPLE_CONTEXT_DIR));
				setGitReference(this.source.getAnnotation(ANNOTATION_SAMPLE_REF));
			}
		}
	}
	
	private void loadBuilderImageMetadata() {
		if(source != null && container != null) {
			setImageName(NLS.bind("{0}/{1}", source.getNamespace(), source.getName()));
			Job loadImageJob = new Job("Load builder image metadata...") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if(initializeContainerInfo()) {
						return Status.OK_STATUS;
					}
					return Status.CANCEL_STATUS;
				}
				
			};
			try {
				final IStatus status = WizardUtils.runInWizard(loadImageJob, container);
				if (!status.isOK()) {
					MessageDialog.openWarning(container.getShell(), "Warning",
							NLS.bind("No builder image metadata for {0} could be found.", getImageName()));
				}
			} catch (InvocationTargetException | InterruptedException e) {
				final String message = NLS.bind("Failed to look-up metadata for a builder image {0}", getImageName());
				MessageDialog.openError(container.getShell(), "Error", message);
				OpenShiftUIActivator.getDefault().getLogger().logError(message, e);
			}
		}
	}
	
	@Override
	protected IDockerImageMetadata lookupImageMetadata() {
		if (source == null) {
			return null;
		}
		try {
			Connection conn = ConnectionsRegistryUtil.getConnectionFor(getProject());
			IResource istag = conn.getResource(ResourceKind.IMAGE_STREAM_TAG, source.getNamespace(), source.getName());
			return new ImageStreamTagMetaData(istag.toJson(true));
		}catch(Exception e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(NLS.bind("Unable to retrieve imagestream tag for {0}", getImageName()), e);
		}
		return null;
	}
	
	@Override
	public String getBuilderImageName() {
		return source.getName();
	}


	@Override
	public String getBuilderImageNamespace() {
		return source.getNamespace();
	}


	public void setContainer(IWizardContainer container) {
		this.container = container;
	}
	
	@Override
	public IEnvironmentVariablesPageModel getEnvVariablesModel() {
		return envModel;
	}

	@Override
	public void setConfigWebHook(boolean value) {
		firePropertyChange(PROPERTY_CONFIG_WEB_HOOK, configWebHook, this.configWebHook = value);
	}

	@Override
	public boolean isConfigWebHook() {
		return this.configWebHook;
	}

	@Override
	public void setConfigChangeTrigger(boolean value) {
		firePropertyChange(PROPERTY_CONFIG_CHANGE_TRIGGER, configChangeTrigger, this.configChangeTrigger = value);
	}

	@Override
	public boolean isConfigChangeTrigger() {
		return configChangeTrigger;
	}

	@Override
	public void setImageChangeTrigger(boolean value) {
		firePropertyChange(PROPERTY_IMAGE_CHANGE_TRIGGER, imageChangeTrigger, this.imageChangeTrigger = value);
	}

	@Override
	public boolean isImageChangeTrigger() {
		return imageChangeTrigger;
	}

	@Override
	public String getGitRepositoryUrl() {
		return this.gitRepositoryUrl;
	}

	@Override
	public void setGitRepositoryUrl(String url) {
		firePropertyChange(PROPERTY_GIT_REPOSITORY_URL, this.gitRepositoryUrl, this.gitRepositoryUrl = url);
	}

	@Override
	public String getGitReference() {
		return this.gitReference;
	}

	@Override
	public void setGitReference(String ref) {
		firePropertyChange(PROPERTY_GIT_REFERENCE, this.gitReference, this.gitReference = ref);
	}

	@Override
	public String getContextDir() {
		return this.contextDir;
	}

	@Override
	public void setContextDir(String contextDir) {
		firePropertyChange(PROPERTY_CONTEXT_DIR, this.contextDir, this.contextDir = contextDir);
	}
	
	


}
