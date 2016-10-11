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

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createConnection;
import static org.jboss.tools.openshift.test.util.ResourceMocks.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.openshift.core.ICommonAttributes;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.job.DeployImageJob;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;
import org.jboss.tools.openshift.test.util.ResourceMocks.IResourceVisitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.internal.restclient.ResourceFactory;
import com.openshift.restclient.IClient;
import com.openshift.restclient.IResourceFactory;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;

/**
 * 
 * @author jeff.cantrill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeployImageJobTest {

	private static final String NAMESPACE = "aProjectName";
	private static final String IMAGE_STREAM_NAME = "somename";
	private static final String RESOURCE_NAME = "myapplication";
	private static final DockerImageURI DOCKER_TAG = new DockerImageURI("repo/mynamespace/myimagename:latest");
	private static final DockerImageURI DOCKER_NEW_TAG = new DockerImageURI("repo/mynamespace/myimagename:v1");
	private static final DockerImageURI DOCKER_TAG_DIFF_REPO = new DockerImageURI("alt-repo/mynamespace/myimagename:latest");
	private TestDeployImageJob job;
	@Mock
	private IClient client;
	@Mock
	private IProject project;
	private IResourceFactory factory;
	@Mock
	private IDeployImageParameters parameters;
	@Mock
	private IProgressMonitor monitor;
	private Connection connection;

	@Before
	public void setUp() throws Exception {
		when(project.getName()).thenReturn(NAMESPACE);
		when(parameters.getProject()).thenReturn(project);
		when(parameters.getReplicas()).thenReturn(5);
		when(parameters.getResourceName()).thenReturn("aresourcename");
		when(client.getOpenShiftAPIVersion()).thenReturn("v1");
		factory = new ResourceFactory(client);
		job = spy(new TestDeployImageJob(parameters));
	}
	
	@Test
	public void shouldUpdateImageTagIfItDifferesFromExisting () {
		givenAConnection();
		givenTheImageStreamExistsTo("myimagename");
		givenTheDeploymentConfigExistFor("myimagename", "v0", NAMESPACE, ResourceKind.IMAGE_STREAM_TAG);
		doReturn(DOCKER_NEW_TAG).when(job).getSourceImage();
		
		IResource resource = connection.getResource(ResourceKind.DEPLOYMENT_CONFIG, 
				project.getName(), parameters.getResourceName());
		IDeploymentImageChangeTrigger trigger = (IDeploymentImageChangeTrigger)((IDeploymentConfig)resource).getTriggers().toArray()[0];
		assertThat(job.doRun(monitor)).isEqualTo(Status.OK_STATUS);
		verify(trigger, times(1)).setFrom(new DockerImageURI(null, null, "myimagename", "v1"));
	}
	
	@Test
	public void shouldNotUpdateIfNoImageChangeTrigger() {
		givenAConnection();
		givenTheImageStreamExistsTo("myimagename");
		IDeploymentConfig dc = createResource(IDeploymentConfig.class);
		when(dc.getTriggers()).thenReturn(Collections.EMPTY_LIST);
		when(connection.getResource(
				ResourceKind.DEPLOYMENT_CONFIG, 
				project.getName(), 
				parameters.getResourceName())).thenReturn(dc);
		assertFalse(job.updateTriggerIfUpdate(connection, project.getName(), parameters.getResourceName()));
	}
	
	@Test
	public void shouldNotUpdateIfImageChangeTriggerHasWrongKind() {
		givenAConnection();
		givenTheImageStreamExistsTo("myimagename");
		givenTheDeploymentConfigExistFor("myimagename", "v0", NAMESPACE, "any-not-IMAGE_STREAM_TAG-kind");
		assertFalse(job.updateTriggerIfUpdate(connection, project.getName(), parameters.getResourceName()));
	}
	
	@Test
	public void shouldNotUpdateIfImageChangeTriggerNameSpaceIsBlank() {
		givenAConnection();
		givenTheImageStreamExistsTo("myimagename");
		givenTheDeploymentConfigExistFor("myimagename", "v0", "", ResourceKind.IMAGE_STREAM_TAG);
		assertFalse(job.updateTriggerIfUpdate(connection, project.getName(), parameters.getResourceName()));
	}
	
	@Test
	public void shouldNotUpdateIfNoImageStreamForTrigger() {
		givenAConnection();
		givenTheDeploymentConfigExistFor("myimagename", "v0", NAMESPACE, ResourceKind.IMAGE_STREAM_TAG);
		assertFalse(job.updateTriggerIfUpdate(connection, project.getName(), parameters.getResourceName()));
	}
	
	@Test
	public void shouldSkipGeneratingResourcesWhenTheImageIsBeingUpdated() {
		givenAConnection();
		givenTheImageStreamExistsTo("myimagename");
		givenTheDeploymentConfigExistFor("myimagename", "v0", NAMESPACE, ResourceKind.IMAGE_STREAM_TAG);
		doReturn(DOCKER_TAG).when(job).getSourceImage();
		assertThat(job.doRun(monitor)).isEqualTo(Status.OK_STATUS);
		verify(connection, times(0)).createResource(any());
	}	
	
	private void givenAConnection() {
		connection = createConnection("https://somehost", "somedevuser");
		when(parameters.getConnection()).thenReturn(connection);
	}
	
	private void givenTheImageStreamExistsTo(String name) {
		IImageStream is = createResource(IImageStream.class);
		when(connection.getResource(ResourceKind.IMAGE_STREAM, project.getName(), name)).thenReturn(is);
	}

	private void givenTheDeploymentConfigExistFor(String name, String tag, String triggerNamespace, String triggerKind) {
		IDeploymentImageChangeTrigger trigger = mock(IDeploymentImageChangeTrigger.class);
		when(trigger.getType()).thenReturn(DeploymentTriggerType.IMAGE_CHANGE);
		when(trigger.getNamespace()).thenReturn(triggerNamespace);
		when(trigger.getKind()).thenReturn(triggerKind);
		when(trigger.getFrom()).thenReturn(new DockerImageURI(null, null, name, tag));
		IDeploymentConfig dc = createResource(IDeploymentConfig.class, new IResourceVisitor<IDeploymentConfig>() {
			@Override
			public void visit(IDeploymentConfig resource) {
				Collection<IDeploymentTrigger> triggers = new ArrayList<IDeploymentTrigger>();
				triggers.add(trigger);
				when(resource.getTriggers()).thenReturn(triggers);
			}
		});
		when(connection.getResource(ResourceKind.DEPLOYMENT_CONFIG, project.getName(), parameters.getResourceName())).thenReturn(dc);
	}

	@Test
	public void testStubImageStreamWhereOneAlreadyExistsInTheProject() {
		givenAnImageStreamTo(project.getName(), DOCKER_TAG);
		IImageStream is = whenStubbingTheImageStream();
		assertNotNull("Exp. an IS to be returned", is);
		assertEquals(IMAGE_STREAM_NAME, is.getName());
		assertEquals(project.getName(), is.getNamespace());
		assertEquals(DOCKER_TAG, is.getDockerImageRepository());
	}

	@Test
	public void testStubImageStreamWhereOneAlreadyExistsInTheCommonProject() {
		givenAnImageStreamTo(ICommonAttributes.COMMON_NAMESPACE, DOCKER_TAG);
		IImageStream is = whenStubbingTheImageStream(DOCKER_TAG_DIFF_REPO);
		assertNotNull("Exp. an IS to be returned", is);
		assertEquals(IMAGE_STREAM_NAME, is.getName());
		assertEquals(ICommonAttributes.COMMON_NAMESPACE, is.getNamespace());
		assertEquals(DOCKER_TAG, is.getDockerImageRepository());
	}

	@Test
	public void testStubImageStreamWhereOneDoesNotExistAndImageIsPublic() {
		givenAnImageStreamTo("foo", null);
		givenTheImageIsVisible(true);

		IImageStream is = whenStubbingTheImageStream();
		assertNotNull("Exp. an IS to be returned", is);
		assertEquals(RESOURCE_NAME, is.getName());
		assertEquals(project.getName(), is.getNamespace());
		assertEquals(DOCKER_TAG, is.getDockerImageRepository());
	}

	@Test
	public void testStubImageStreamWhereOneDoesNotExistAndImageIsLocal() {

		givenAnImageStreamTo("foo", null);
		givenTheImageIsVisible(false);
		
		assertNull(whenStubbingTheImageStream());
	}
	
	private void givenTheImageIsVisible(boolean visible) {
		doReturn(visible).when(job).isImageVisibleByOpenShift(any(), any());
	}
	
	private IImageStream givenAnImageStreamTo(String namespace, DockerImageURI uri) {
		IImageStream is = mock(IImageStream.class);
		when(is.getNamespace()).thenReturn(namespace);
		when(is.getName()).thenReturn(IMAGE_STREAM_NAME);
		when(is.getDockerImageRepository()).thenReturn(uri);

		List<IResource> streams = Arrays.asList(is);

		Connection conn = mock(Connection.class);
		when(conn.getResources(anyString(), eq(namespace))).thenReturn(streams);
		when(conn.getClusterNamespace()).thenReturn(ICommonAttributes.COMMON_NAMESPACE);
		when(parameters.getConnection()).thenReturn(conn);
		return is;
	}
	
	private IImageStream whenStubbingTheImageStream() {
		return whenStubbingTheImageStream(DOCKER_TAG);
	}
	private IImageStream whenStubbingTheImageStream(DockerImageURI uri) {
		return (IImageStream) job.stubImageStream(factory, RESOURCE_NAME, project, uri);
	}
	
	@Test
	public void testStubDeploymentConfig() {
		IImageStream is  = givenAnImageStreamTo(project.getName(), DOCKER_TAG);
		
		IResource resource = job.stubDeploymentConfig(factory, RESOURCE_NAME, DOCKER_TAG, is);
		assertTrue(resource instanceof IDeploymentConfig);
		IDeploymentConfig dc = (IDeploymentConfig) resource;

		assertEquals("Exp. replicas to match incoming params",parameters.getReplicas(), dc.getReplicas());
		assertEquals("Exp. the selector key to be the resourceName", RESOURCE_NAME, dc.getReplicaSelector().get(DeployImageJob.SELECTOR_KEY));
		
		IContainer container = dc.getContainer(RESOURCE_NAME);
		assertNotNull("Exp. to find a container with the resource name", container);
		Collection<IDeploymentTrigger> triggers = dc.getTriggers();
		assertTrue("Exp. a config change trigger", triggers
					.stream().filter(t->DeploymentTriggerType.CONFIG_CHANGE.equals(t.getType())).findFirst().isPresent());
		
		//assert ict matches container spec
		Optional<IDeploymentTrigger> icTrigger = triggers.stream().filter(t->DeploymentTriggerType.IMAGE_CHANGE.equals(t.getType())).findFirst();
		assertTrue(icTrigger.isPresent());
		
		IDeploymentImageChangeTrigger imageChangeTrigger = (IDeploymentImageChangeTrigger)icTrigger.get();
		Collection<String> names = imageChangeTrigger.getContainerNames();
		assertEquals(1, names.size());
		assertEquals("Exp. the container and trigger names to match", container.getName(), names.iterator().next());
		assertTrue(imageChangeTrigger.isAutomatic());
		assertEquals(ResourceKind.IMAGE_STREAM_TAG, imageChangeTrigger.getKind());
		assertEquals("Exp. the trigger to point to the imagestream name", new DockerImageURI(null, null, is.getName(), DOCKER_TAG.getTag()), imageChangeTrigger.getFrom());
		assertEquals("Exp. the trigger to point to the imagestream name", is.getNamespace(), imageChangeTrigger.getNamespace());
	}
	
	public static class TestDeployImageJob extends DeployImageJob{

		public TestDeployImageJob(IDeployImageParameters parameters) {
			super(parameters);
		}
		
		@Override
		protected DockerImageURI getSourceImage() {
			return super.getSourceImage();
		}

		@Override
		protected IStatus doRun(IProgressMonitor monitor) {
			return super.doRun(monitor);
		}
		
		@Override
		protected boolean updateTriggerIfUpdate(Connection connection, String project, String name) {
			return super.updateTriggerIfUpdate(connection, project, name);
		}

		@Override
		public IImageStream stubImageStream(IResourceFactory factory, String name, IProject project,
				DockerImageURI imageUri) {
			return super.stubImageStream(factory, name, project, imageUri);
		}

		@Override
		public IResource stubDeploymentConfig(IResourceFactory factory, String name, DockerImageURI imageUri, IImageStream is) {
			return super.stubDeploymentConfig(factory, name, imageUri, is);
		}

		@Override
		public boolean isImageVisibleByOpenShift(IProject project, DockerImageURI uri) {
			return super.isImageVisibleByOpenShift(project, uri);
		}
		
		
	}
}
