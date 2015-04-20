/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.StyledString;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.explorer.ResourceGrouping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

@RunWith(MockitoJUnitRunner.class)
/*
 * Skipping getImage tests as they can't be run headless
 */
public class OpenShiftExplorerLabelProviderTest {

	private OpenShiftExplorerLabelProvider provider;
	@Mock IClient client;
	
	@Before
	public void setup() throws MalformedURLException{
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8443"));
		provider = new OpenShiftExplorerLabelProvider();
	}
	private <T extends IResource> T givenAResource(Class<T> klass, ResourceKind kind){
		T resource = mock(klass);
		when(resource.getKind()).thenReturn(kind);
		when(resource.getName()).thenReturn("someName");
		return resource;
	}
	@Test
	public void getStyledTextForABuild(){
		IBuild build = givenAResource(IBuild.class, ResourceKind.Build);
		when(build.getStatus()).thenReturn("Running");
		
		assertEquals(String.format("%s Running", build.getName()), provider.getStyledText(build).getString());
	}
	
	@Test
	public void getStyledTextForAReplicationController(){
		IReplicationController rc = givenAResource(IReplicationController.class, ResourceKind.ReplicationController);
		Map<String, String> selector = new HashMap<String, String>();
		selector.put("foo", "bar");
		when(rc.getReplicaSelector()).thenReturn(selector);
		
		assertEquals(String.format("%s selector: foo=bar", rc.getName()), provider.getStyledText(rc).getString());
	}

	@Test
	public void getStyledTextForAPod(){
		IPod pod = givenAResource(IPod.class, ResourceKind.Pod);
		when(pod.getIP()).thenReturn("172.17.2.226");
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("foo", "bar");
		when(pod.getLabels()).thenReturn(labels);
		
		String exp = String.format("%s labels: %s", pod.getName(), StringUtils.serialize(pod.getLabels()));
		assertEquals(exp, provider.getStyledText(pod).getString());
	}

	@Test
	public void getStyledTextForAPodWithoutLabels(){
		IPod pod = givenAResource(IPod.class, ResourceKind.Pod);
		when(pod.getIP()).thenReturn("172.17.2.226");
		Map<String, String> labels = new HashMap<String, String>();
		when(pod.getLabels()).thenReturn(labels);
		
		assertEquals(pod.getName(), provider.getStyledText(pod).getString());
	}
	
	@Test
	public void getStyledTextForAService(){
		IService service = givenAResource(IService.class, ResourceKind.Service);
		when(service.getPortalIP()).thenReturn("172.17.2.226");
		when(service.getPort()).thenReturn(5432);
		when(service.getContainerPort()).thenReturn(3306);
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("foo", "bar");
		when(service.getSelector()).thenReturn(labels);
		
		String exp = String.format("%s selector: foo=bar", service.getName());
		assertEquals(exp, provider.getStyledText(service).getString());
	}
	@Test
	public void getStyledTextForAnImageRepository(){
		IImageStream repo = givenAResource(IImageStream.class, ResourceKind.ImageStream);
		when(repo.getDockerImageRepository())
			.thenReturn(new DockerImageURI("127.0.0.1", "foo", "bar"));
		assertEquals(repo.getName() +" " + repo.getDockerImageRepository(), provider.getStyledText(repo).getString());
	}
	
	@Test
	public void getStyledTextForADeploymentConfig(){
		IDeploymentConfig config = givenAResource(IDeploymentConfig.class, ResourceKind.DeploymentConfig);
		Map<String, String> selector = new HashMap<String, String>();
		selector.put("name", "foo");
		selector.put("deployment", "bar");
		when(config.getReplicaSelector()).thenReturn(selector );
		
		assertEquals(config.getName() + " selector: deployment=bar,name=foo", provider.getStyledText(config).getString());
	}
	
	@Test
	public void getStyledTextForABuildConfig(){
		IBuildConfig buildConfig = givenAResource(IBuildConfig.class, ResourceKind.BuildConfig);
		when(buildConfig.getSourceURI()).thenReturn("git://somplace.com/foo/bar.git");
		
		StyledString actual = provider.getStyledText(buildConfig);
		assertEquals(buildConfig.getName() + " git://somplace.com/foo/bar.git", actual.getString());
	}
	
	@Test
	public void getStyledTextForAProjectWithoutDisplayName(){
		String displayName = "The Display Name";
		String namespace = "anamespace";
		
		IProject project = givenAResource(IProject.class, ResourceKind.Project);
		when(project.getName()).thenReturn(displayName);
		when(project.getNamespace()).thenReturn(namespace);

		assertEquals(String.format("%s ns: %s", project.getName(), project.getNamespace()), provider.getStyledText(project).getString());
	}

	@Test
	public void getStyledTextForAProject(){
		String displayName = "The Display Name";
		String namespace = "anamespace";
		
		IProject project = givenAResource(IProject.class, ResourceKind.Project);
		when(project.getDisplayName()).thenReturn(displayName);
		when(project.getNamespace()).thenReturn(namespace);
		
		assertEquals(String.format("%s ns: %s", project.getDisplayName(), project.getNamespace()), provider.getStyledText(project).getString());
	}
	
	@Test
	public void getStyledTextForAProjectWithoutNamespace(){
		String displayName = "The Display Name";
		String namespace = "";
		
		IProject project = givenAResource(IProject.class, ResourceKind.Project);
		when(project.getDisplayName()).thenReturn(displayName);
		when(project.getNamespace()).thenReturn(namespace);
		
		StyledString exp = new StyledString(displayName);
		StyledString actual = provider.getStyledText(project);
		assertEquals(exp.getString(), actual.getString());
	}
	
	@Test
	public void getStyledTextForResourceGrouping(){
		ResourceGrouping grouping = new ResourceGrouping(ResourceKind.Service, mock(IProject.class));
		assertEquals("Services", provider.getStyledText(grouping).getString());
	}
	
	@Test
	public void getStyledTextForAConnection(){
		Connection connection = new Connection(client, null, null, null);
		connection.setUsername("foo@bar.com");
		String exp = String.format("foo@bar.com %s", client.getBaseURL().toString());
		assertEquals("Exp. a connection to display its base URL", exp, provider.getStyledText(connection).getString());
	}

}
