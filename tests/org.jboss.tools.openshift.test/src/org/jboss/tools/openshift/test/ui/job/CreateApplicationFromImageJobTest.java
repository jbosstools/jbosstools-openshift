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
package org.jboss.tools.openshift.test.ui.job;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.job.CreateApplicationFromImageJob;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.common.IEnvironmentVariablesPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.IBuildConfigPageModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.build.IBuildConfigBuilder;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;

@RunWith(MockitoJUnitRunner.class)
public class CreateApplicationFromImageJobTest {
	
	protected static final String PROJECT_NAME = "theProjectName";
	private static final String GIT_URL = "https://someplace/username/project.git";
	private static final String GIT_REF = "release1.1";
	private static final String CONTEXT_DIR = "afolder/inrepo";
	private static final String IS_TAG = "imagename:1.0";
	private static final String IS_NAMESPACE = "someothernamespace";
	private static final List<EnvironmentVariable> BC_ENV_VARS = new ArrayList<>();
	private static final List<IEnvironmentVariable> CONV_ENV_VARS = new ArrayList<>();
	private static final String APP_NAME = "myappname";

	private TestCreateApplicationFromImageJob job;

	@Mock private IBuildConfigPageModel bcModel;
	@Mock private IDeployImageParameters deployImageModel;
	@Mock private IResourceFactory factory;
	@Mock private IProject project;
	@Mock private IBuildConfig bc;
	@Mock private IBuildConfigBuilder builder;
	@Mock private IBuildConfigBuilder.IGitSourceBuilder sourceBuilder;
	@Mock private IBuildConfigBuilder.ISourceStrategyBuilder sourceStratBuilder;
	@Mock private IClient client;
	private Connection connection;
	
	@Before
	public void setUp() throws Exception {
		when(project.getName()).thenReturn(PROJECT_NAME);
		job = new TestCreateApplicationFromImageJob(bcModel, deployImageModel);
		connection = spy(new Connection(client, null, null));
		doReturn(builder).when(connection).getResourceBuilder(any());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testStubDeploymentConfig() {
		IDeploymentConfig stub = mock(IDeploymentConfig.class);
		when(stub.getName()).thenReturn(APP_NAME);
		when(stub.addTrigger(anyString())).thenReturn(mock(IDeploymentImageChangeTrigger.class));
		when(factory.stub(anyString(), anyString(), anyString())).thenReturn(stub);
		when(deployImageModel.getProject()).thenReturn(project);
		when(deployImageModel.getLabels()).thenReturn(Collections.emptyList());
		when(deployImageModel.getVolumes()).thenReturn(Collections.emptyList());
		when(deployImageModel.getPortSpecs()).thenReturn(Collections.emptyList());
		when(deployImageModel.getReplicas()).thenReturn(1);
		when(deployImageModel.getEnvironmentVariables()).thenReturn(Collections.emptyList());
		
		job.stubDeploymentConfig(factory, APP_NAME, new DockerImageURI("foo:bar"));
		
		verify(stub).addContainer(eq(APP_NAME), eq(new DockerImageURI(APP_NAME)), anySet(), anyMap(), anyList());
		
	}
	
	@Test
	public void testStubImageStream() {
		IImageStream stub = mock(IImageStream.class);
		when(factory.stub(anyString(), anyString(), anyString())).thenReturn(stub);

		IImageStream is = job.stubImageStream(factory, APP_NAME, project, null);
		
		assertEquals(stub, is);
		verify(factory).stub(ResourceKind.IMAGE_STREAM, APP_NAME, project.getName());
		
	}
	
	@Test
	public void testBuildConfigShouldBeCreatedWhenGeneratingResources() {
		//setup
		givenBuildConfig();
		givenBuildConfigModel();
		Map<String, IResource> resources = new HashMap<>();
		
		
		//when
		job.addToGeneratedResources(resources, connection, APP_NAME, project);
		
		//then
		assertTrue("Exp. a build config to be created", resources.containsKey(ResourceKind.BUILD_CONFIG));
		assertEquals(bc, resources.get(ResourceKind.BUILD_CONFIG));
		
		verify(sourceBuilder).fromGitUrl(GIT_URL);
		verify(sourceBuilder).usingGitReference(GIT_REF);
		verify(sourceBuilder).inContextDir(CONTEXT_DIR);
		verify(sourceStratBuilder).fromImageStreamTag(IS_TAG);
		verify(sourceStratBuilder).inNamespace(IS_NAMESPACE);
		verify(sourceStratBuilder).withEnvVars(CONV_ENV_VARS);
		verify(builder).named(APP_NAME);
		verify(builder).inNamespace(project.getName());
		verify(builder).buildOnSourceChange(true);
		verify(builder).buildOnConfigChange(true);
		verify(builder).buildOnImageChange(true);
		verify(builder).toImageStreamTag(APP_NAME + ":latest");
		verify(builder).build();
	}
	
	private void givenBuildConfigModel() {
		IEnvironmentVariablesPageModel envModel = mock(IEnvironmentVariablesPageModel.class);
		when(bcModel.getEnvVariablesModel()).thenReturn(envModel);
		when(bcModel.getGitRepositoryUrl()).thenReturn(GIT_URL);
		when(bcModel.getGitReference()).thenReturn(GIT_REF);
		when(bcModel.getContextDir()).thenReturn(CONTEXT_DIR);
		when(bcModel.getBuilderImageName()).thenReturn(IS_TAG);
		when(bcModel.getBuilderImageNamespace()).thenReturn(IS_NAMESPACE);
		when(envModel.getEnvironmentVariables()).thenReturn(BC_ENV_VARS);
		when(bcModel.isConfigChangeTrigger()).thenReturn(true);
		when(bcModel.isImageChangeTrigger()).thenReturn(true);
		when(bcModel.isConfigWebHook()).thenReturn(true);
	}

	private void givenBuildConfig() {
		when(factory.stub(anyString(), anyString(), anyString())).thenReturn(bc);
		when(builder.build()).thenReturn(bc);
		when(builder.named(anyString())).thenReturn(builder);
		when(builder.inNamespace(anyString())).thenReturn(builder);
		when(builder.buildOnSourceChange(anyBoolean())).thenReturn(builder);
		when(builder.buildOnConfigChange(anyBoolean())).thenReturn(builder);
		when(builder.buildOnImageChange(anyBoolean())).thenReturn(builder);
		when(builder.toImageStreamTag(anyString())).thenReturn(builder);
		
		when(builder.fromGitSource()).thenReturn(sourceBuilder);
		when(sourceBuilder.fromGitUrl(anyString())).thenReturn(sourceBuilder);
		when(sourceBuilder.usingGitReference(anyString())).thenReturn(sourceBuilder);
		when(sourceBuilder.inContextDir(anyString())).thenReturn(sourceBuilder);
		when(sourceBuilder.end()).thenReturn(builder);
		
		when(builder.usingSourceStrategy()).thenReturn(sourceStratBuilder);
		when(sourceStratBuilder.fromImageStreamTag(anyString())).thenReturn(sourceStratBuilder);
		when(sourceStratBuilder.inNamespace(anyString())).thenReturn(sourceStratBuilder);
		when(sourceStratBuilder.withEnvVars(any())).thenReturn(sourceStratBuilder);
		when(sourceStratBuilder.end()).thenReturn(builder);
		
	}
	
	static class TestCreateApplicationFromImageJob extends CreateApplicationFromImageJob{
		
		public TestCreateApplicationFromImageJob(IBuildConfigPageModel buildConfigModel, IDeployImageParameters deployImageModel){
			super(buildConfigModel, deployImageModel);
		}
		public void addToGeneratedResources(Map<String, IResource> resources, final Connection connection, final String name, final IProject project) {
			super.addToGeneratedResources(resources, connection, name, project);
		}
		
		public IImageStream stubImageStream(IResourceFactory factory, String name, IProject project,
				DockerImageURI imageUri) {
			return super.stubImageStream(factory, name, project, imageUri); 
		}

	}
}
