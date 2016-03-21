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

import java.util.Collection;
import java.util.Optional;

import org.jboss.tools.openshift.internal.ui.job.DeployImageJob;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeployImageParameters;
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
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.deploy.DeploymentTriggerType;
import com.openshift.restclient.model.deploy.IDeploymentTrigger;

/**
 * 
 * @author jeff.cantrill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DeployImageJobTest {

	private static final String RESOURCE_NAME = "myapplication";
	private static final DockerImageURI DOCKER_TAG = new DockerImageURI("mynamespace/myimagename:latest");
	private DeployImageJob job;
	@Mock
	private IClient client;
	@Mock
	private IProject project;
	private IResourceFactory factory;
	@Mock
	private IDeployImageParameters parameters;

	@Before
	public void setUp() throws Exception {
		when(project.getName()).thenReturn("aProjectName");
		when(parameters.getProject()).thenReturn(project);
		when(parameters.getReplicas()).thenReturn(5);
		when(client.getOpenShiftAPIVersion()).thenReturn("v1");
		factory = new ResourceFactory(client);
		job = new DeployImageJob(parameters);
	}

	@Test
	public void testStubDeploymentConfig() {
		IResource resource = job.stubDeploymentConfig(factory, RESOURCE_NAME, DOCKER_TAG);
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
		assertEquals("Exp. the trigger to point to the imagestream name", new DockerImageURI(null, null, RESOURCE_NAME, DOCKER_TAG.getTag()), imageChangeTrigger.getFrom());
	}

}
