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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.explorer.NewProjectLinkNode;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.models.ResourceWrapper;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.ImageStreamApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

	private <T extends IResource> ResourceWrapper givenAResourceUIModel(Class<T> klass, String kind){
		T resource = givenAResource(klass, kind);
		ResourceWrapper resourceUIModel = mock(ResourceWrapper.class);
		when(resourceUIModel.getResource()).thenReturn(resource);
		when(resourceUIModel.getAdapter((Class<?>)Mockito.any(Class.class))).then(invocation-> {
			if (invocation.getArguments()[0] == IResource.class) {
				return resource;
			}
			return null;
		});
		return resourceUIModel;
	}

	@Test
	public void getStyledTextForABuild(){
		IBuild build = givenAResource(IBuild.class, ResourceKind.BUILD);
		when(build.getStatus()).thenReturn("Running");
		
		assertEquals(String.format("%s Build Running", build.getName()), provider.getStyledText(build).getString());
	}

	@Test
	public void getStyledTextForAnApplicationSource(){
		IApplicationSource source = mock(IApplicationSource.class);
		when(source.getTags()).thenReturn(Arrays.asList("foo","bar"));
		when(source.getNamespace()).thenReturn("aNamespace");
		when(source.getName()).thenReturn("aname");
		
		assertEquals("", "aname (foo, bar) - aNamespace", provider.getStyledText(source).getString());
	}

	@Test
	public void getStyledTextForAnApplicationSourceWithoutTags(){
		IApplicationSource source = mock(IApplicationSource.class);
		when(source.getTags()).thenReturn(Collections.emptyList());
		when(source.getNamespace()).thenReturn("aNamespace");
		when(source.getName()).thenReturn("aname");
	
		assertEquals("", "aname () - aNamespace", provider.getStyledText(source).getString());
	}

	@Test
	public void getStyledTextForARoute() {
		IRoute route = givenAResource(IRoute.class, ResourceKind.ROUTE);
		when(route.getHost()).thenReturn("www.example.com");
		when(route.getPath()).thenReturn("");
		assertEquals(String.format("%s www.example.com", route.getName()), provider.getStyledText(route).getString());

		when(route.getPath()).thenReturn("/foo");
		assertEquals(String.format("%s www.example.com/foo", route.getName()), provider.getStyledText(route).getString());
		//test description
		assertEquals(String.format("%s www.example.com/foo", route.getName()), provider.getDescription(route));
	}
	
	@Test
	public void getStyledTextForAReplicationController(){
		IReplicationController rc = givenAResource(IReplicationController.class, ResourceKind.REPLICATION_CONTROLLER);
		Map<String, String> selector = new HashMap<>();
		selector.put("foo", "bar");
		when(rc.getReplicaSelector()).thenReturn(selector);
		
		assertEquals(String.format("%s selector: foo=bar", rc.getName()), provider.getStyledText(rc).getString());
	}

	@Test
	public void getStyledTextForAPod(){
		IPod pod = givenAResource(IPod.class, ResourceKind.POD);
		assertEquals(String.format("%s Pod", pod.getName()), provider.getStyledText(pod).getString());
		
		String status = "Chilling";
		when(pod.getStatus()).thenReturn(status);
		String exp = String.format("%s Pod %s", pod.getName(), status);
		assertEquals(exp, provider.getStyledText(pod).getString());
	}

	@Test
	public void getStyledTextForAPodWithLongNames() {
		provider.setLabelLimit(10);
		IPod pod = givenAResource(IPod.class, ResourceKind.POD);

		String status = "Chilling";
		when(pod.getStatus()).thenReturn(status);
		String exp = "s...e P...g";
		assertEquals(exp, provider.getStyledText(pod).getString());
		//test description
		assertEquals("someName Pod Chilling", provider.getDescription(pod));
	}
	
	@Test
	public void getStyledTextForAPodWithoutLabels(){
		IPod pod = givenAResource(IPod.class, ResourceKind.POD);
		when(pod.getIP()).thenReturn("172.17.2.226");
		Map<String, String> labels = new HashMap<>();
		when(pod.getLabels()).thenReturn(labels);
		
		assertEquals(String.format("%s Pod", pod.getName()), provider.getStyledText(pod).getString());
	}
	
	@Test
	public void getStyledTextForAService(){
		IService service = givenAResource(IService.class, ResourceKind.SERVICE);
		when(service.getPortalIP()).thenReturn("172.17.2.226");
		when(service.getPort()).thenReturn(5432);
		when(service.getTargetPort()).thenReturn("3306");
		Map<String, String> labels = new HashMap<>();
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
		Map<String, String> selector = new HashMap<>();
		selector.put("name", "foo");
		selector.put("deployment", "bar");
		when(config.getReplicaSelector()).thenReturn(selector );
		
		assertEquals(config.getName() + " selector: deployment=bar,name=foo", provider.getStyledText(config).getString());
	}
	
	@Test
	public void getStyledTextForADeploymentConfigWithLongNames(){
		provider.setLabelLimit(120);
		IDeploymentConfig config = givenAResource(IDeploymentConfig.class, ResourceKind.DEPLOYMENT_CONFIG);
		Map<String, String> selector = new HashMap<>();
		String name = "foo01234567890123456789012345678901234567890123456789";
		selector.put("name", name);
		String deployment = "bar01234567890123456789012345678901234567890123456789";
		selector.put("deployment", deployment);
		when(config.getReplicaSelector()).thenReturn(selector );
		assertEquals("someName selector: deployment=bar0123456789012345678901234567890...=foo01234567890123456789012345678901234567890123456789", provider.getStyledText(config).getString());
		//test description
		assertEquals("someName selector: deployment=" + deployment + ",name=" + name, provider.getDescription(config));
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

	@Test
	public void getImage(){
		testImage(null, null);
		testImage(null, new Object());
		testImage(OpenShiftImages.SERVICE_IMG, IService.class, ResourceKind.SERVICE);
		testImage(OpenShiftImages.PROJECT_NEW_IMG, mock(NewProjectLinkNode.class));
		testImage(OpenShiftImages.BUILD_IMG, IBuild.class, ResourceKind.BUILD);
		testImage(OpenShiftImages.BUILDCONFIG_IMG, IBuild.class, ResourceKind.BUILD_CONFIG);
		testImage(OpenShiftImages.IMAGE_IMG, IBuild.class, ResourceKind.IMAGE_STREAM);
		testImage(OpenShiftImages.BLOCKS_IMG, IBuild.class, ResourceKind.POD);
		testImage(OpenShiftImages.PROJECT_IMG, IBuild.class, ResourceKind.PROJECT);
		testImage(OpenShiftImages.ROUTE_IMG, IBuild.class, ResourceKind.ROUTE);
		testImage(OpenShiftImages.SERVICE_IMG, IBuild.class, ResourceKind.SERVICE);
		testImage(OpenShiftCommonImages.FILE, IResource.class, "unknown");
		testImage(OpenShiftImages.IMAGE_IMG, mock(ImageStreamApplicationSource.class));
		testImage(OpenShiftImages.TEMPLATE_IMG, mock(TemplateApplicationSource.class));
	}

	private void testImage(Image expectedImage, Object element) {
		assertEquals(expectedImage, provider.getImage(element));
	}

	private void testImage(Image expectedImage, Class<? extends IResource> klass, String kind) {
		testImage(expectedImage, givenAResource(klass, kind));
		testImage(expectedImage, givenAResourceUIModel(klass, kind));
	}
}
