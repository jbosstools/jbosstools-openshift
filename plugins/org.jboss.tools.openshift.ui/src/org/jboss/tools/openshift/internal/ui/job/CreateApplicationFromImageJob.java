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
package org.jboss.tools.openshift.internal.ui.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.IBuildConfigPageModel;

import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.build.IBuildConfigBuilder;

public class CreateApplicationFromImageJob 
	extends DeployImageJob 
	implements IResourcesModelJob{

	private IBuildConfigPageModel buildConfigModel;

	public CreateApplicationFromImageJob(IBuildConfigPageModel buildConfigModel, IDeployImageParameters deployImageModel){
		super("Create Application From Builder Image Job", deployImageModel);
		this.buildConfigModel = buildConfigModel;
	}
	
	@Override
	public Job getJob() {
		return this;
	}

	@Override
	public Runnable getSummaryRunnable(final Shell shell) {
		return new Runnable() {
			
			@Override
			public void run() {
				String message = NLS.bind(
						"Results of creating the resources from the {0}/{1} builder image.", 
						buildConfigModel.getBuilderImageNamespace(),
						buildConfigModel.getBuilderImageName());
				new ResourceSummaryDialog(shell, getResources(),"Create Application Summary",message).open();
			}
		};
	}
	
	@Override
	protected void addToGeneratedResources(Map<String, IResource> resources, final Connection connection, final String name, final IProject project) {
		IBuildConfigBuilder builder = connection.getResourceBuilder(IBuildConfigBuilder.class);
		IBuildConfig bc = builder
			.named(name)
			.inNamespace(project.getName())
			.fromGitSource()
				.fromGitUrl(buildConfigModel.getGitRepositoryUrl())
				.usingGitReference(buildConfigModel.getGitReference())
				.inContextDir(buildConfigModel.getContextDir())
			.end()
			.usingSourceStrategy()
				.fromImageStreamTag(buildConfigModel.getBuilderImageName())
				.inNamespace(buildConfigModel.getBuilderImageNamespace())
				.withEnvVars(createEnvVars())
			.end()
			.buildOnSourceChange(buildConfigModel.isConfigWebHook())
			.buildOnConfigChange(buildConfigModel.isConfigChangeTrigger())
			.buildOnImageChange(buildConfigModel.isImageChangeTrigger())
			.toImageStreamTag(new DockerImageURI(name).getNameAndTag())
		.build();
		
		resources.put(ResourceKind.BUILD_CONFIG, bc);
	}
	
	
	@Override
	public IResource stubDeploymentConfig(IResourceFactory factory, String name, DockerImageURI imageUri) {
		return super.stubDeploymentConfig(factory, name, new DockerImageURI(name));
	}

	@Override
	protected IImageStream stubImageStream(IResourceFactory factory, String name, IProject project,
			DockerImageURI imageUri) {
		return factory.stub(ResourceKind.IMAGE_STREAM, name, project.getName());
	}

	private List<IEnvironmentVariable> createEnvVars(){
		List<IEnvironmentVariable> list = new ArrayList<>();
		buildConfigModel.getEnvVariablesModel()
			.getEnvironmentVariables()
			.forEach(v-> list.add(
					new IEnvironmentVariable() {
						@Override
						public String getName() {
							return v.getKey();
						}
		
						@Override
						public String getValue() {
							return v.getValue();
						}
		
						@Override
						public IEnvVarSource getValueFrom() {
							return null;
						}

						@Override
						public String toJson() {
							ModelNode node = new ModelNode();
							node.get("name").set(getName());
							node.get("value").set(getValue());
							return node.toJSONString(true);
						}
						
						
					}
			));
		return list;
	}
}
