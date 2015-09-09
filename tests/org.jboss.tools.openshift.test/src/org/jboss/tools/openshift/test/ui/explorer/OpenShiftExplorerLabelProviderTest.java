/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.explorer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.StyledString;
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
import com.openshift.restclient.model.route.IRoute;

/**
 * Skipping getImage tests as they can't be run headless
 * @author jeff.cantrill
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenShiftExplorerLabelProviderTest {

	private OpenShiftExplorerLabelProvider provider;
	@Mock IClient client;
	
	@Before
	public void setup() throws MalformedURLException{
		when(client.getBaseURL()).thenReturn(new URL("https://localhost:8443"));
		provider = new OpenShiftExplorerLabelProvider();
	}
	private <T extends IResource> T givenAResource(Class<T> klass, String kind){
		T resource = mock(klass);
		when(resource.getKind()).thenReturn(kind);
		when(resource.getName()).thenReturn("someName");
		return resource;
	}
	@Test
	public void getStyledTextForABuild(){
		IBuild build = givenAResource(IBuild.class, ResourceKind.BUILD);
		when(build.getStatus()).thenReturn("Running");
		
		assertEquals(String.format("%s Running", build.getName()), provider.getStyledText(build).getString());
	}
	
	@Test
	public void getStyledTextForARoute() {
		IRoute route = givenAResource(IRoute.class, ResourceKind.ROUTE);
		when(route.getHost()).thenReturn("www.example.com");
		when(route.getPath()).thenReturn("");
		assertEquals(String.format("%s www.example.com", route.getName()), provider.getStyledText(route).getString());

		when(route.getPath()).thenReturn("/foo");
		assertEquals(String.format("%s www.example.com/foo", route.getName()), provider.getStyledText(route).getString());
	}
	
	@Test
	public void getStyledTextForAReplicationController(){
		IReplicationController rc = givenAResource(IReplicationController.class, ResourceKind.REPLICATION_CONTROLLER);
		Map<String, String> selector = new HashMap<String, String>();
		selector.put("foo", "bar");
		when(rc.getReplicaSelector()).thenReturn(selector);
		
		assertEquals(String.format("%s selector: foo=bar", rc.getName()), provider.getStyledText(rc).getString());
	}

	@Test
	public void getStyledTextForAPod(){
		IPod pod = givenAResource(IPod.class, ResourceKind.POD);
		assertEquals(pod.getName(), provider.getStyledText(pod).getString());
		
		String status = "Chilling";
		when(pod.getStatus()).thenReturn(status);
		String exp = String.format("%s %s", pod.getName(), status);
		assertEquals(exp, provider.getStyledText(pod).getString());
	}

	@Test
	public void getStyledTextForAPodWithoutLabels(){
		IPod pod = givenAResource(IPod.class, ResourceKind.POD);
		when(pod.getIP()).thenReturn("172.17.2.226");
		Map<String, String> labels = new HashMap<String, String>();
		when(pod.getLabels()).thenReturn(labels);
		
		assertEquals(pod.getName(), provider.getStyledText(pod).getString());
	}
	
	@Test
	public void getStyledTextForAService(){
		IService service = givenAResource(IService.class, ResourceKind.SERVICE);
		when(service.getPortalIP()).thenReturn("172.17.2.226");
		when(service.getPort()).thenReturn(5432);
		when(service.getTargetPort()).thenReturn(3306);
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("foo", "bar");
		when(service.getSelector()).thenReturn(labels);
		
		String exp = String.format("%s selector: foo=bar", service.getName());
		assertEquals(exp, provider.getStyledText(service).getString());
	}
	@Test
	public void getStyledTextForAnImageRepository(){
		IImageStream repo = givenAResource(IImageStream.class, ResourceKind.IMAGE_STREAM);
		when(repo.getDockerImageRepository())
			.thenReturn(new DockerImageURI("127.0.0.1", "foo", "bar"));
		assertEquals(repo.getName() +" " + repo.getDockerImageRepository(), provider.getStyledText(repo).getString());
	}
	
	@Test
	public void getStyledTextForADeploymentConfig(){
		IDeploymentConfig config = givenAResource(IDeploymentConfig.class, ResourceKind.DEPLOYMENT_CONFIG);
		Map<String, String> selector = new HashMap<String, String>();
		selector.put("name", "foo");
		selector.put("deployment", "bar");
		when(config.getReplicaSelector()).thenReturn(selector );
		
		assertEquals(config.getName() + " selector: deployment=bar,name=foo", provider.getStyledText(config).getString());
	}
	
	@Test
	public void getStyledTextForABuildConfig(){
		IBuildConfig buildConfig = givenAResource(IBuildConfig.class, ResourceKind.BUILD_CONFIG);
		when(buildConfig.getSourceURI()).thenReturn("git://somplace.com/foo/bar.git");
		
		StyledString actual = provider.getStyledText(buildConfig);
		assertEquals(buildConfig.getName() + " git://somplace.com/foo/bar.git", actual.getString());
	}
	
	@Test
	public void getStyledTextForAProjectWithoutDisplayName(){
		String displayName = "The Display Name";
		String namespace = "anamespace";
		
		IProject project = givenAResource(IProject.class, ResourceKind.PROJECT);
		when(project.getName()).thenReturn(displayName);
		when(project.getNamespace()).thenReturn(namespace);

		assertEquals(project.getName(), provider.getStyledText(project).getString());
	}

	@Test
	public void getStyledTextForAProject(){
		String displayName = "The Display Name";
		String namespace = "anamespace";
		
		IProject project = givenAResource(IProject.class, ResourceKind.PROJECT);
		when(project.getDisplayName()).thenReturn(displayName);
		when(project.getNamespace()).thenReturn(namespace);
		
		assertEquals(project.getDisplayName() + " " + project.getName(), provider.getStyledText(project).getString());
	}
	
	@Test
	public void getStyledTextForResourceGrouping(){
		ResourceGrouping grouping = new ResourceGrouping(ResourceKind.SERVICE, mock(IProject.class));
		assertEquals("Services", provider.getStyledText(grouping).getString());
	}
	
	@Test
	public void getStyledTextForAConnection(){
		Connection connection = new Connection(client, null, null);
		connection.setUsername("foo@bar.com");
		String exp = String.format("foo@bar.com %s", client.getBaseURL().toString());
		assertEquals("Exp. a connection to display its base URL", exp, provider.getStyledText(connection).getString());
	}
	@Test
	public void getStyledTextForAConnectionWithoutUserName(){
		Connection connection = new Connection(client, null, null);
		String exp = String.format("<unknown user> %s", client.getBaseURL().toString());
		assertEquals("Exp. a connection to display its base URL", exp, provider.getStyledText(connection).getString());
	}

}
