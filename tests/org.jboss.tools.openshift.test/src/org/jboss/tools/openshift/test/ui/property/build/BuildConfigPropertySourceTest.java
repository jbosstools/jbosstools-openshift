/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property.build;

import static org.jboss.tools.openshift.test.ui.property.Assert.assertPropertyDescriptorsContains;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.BuildConfigPropertySource;
import org.jboss.tools.openshift.internal.ui.property.ExtTextPropertyDescriptor;
import org.jboss.tools.openshift.internal.ui.property.KeyValuePropertySource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.BuildSourceType;
import com.openshift.restclient.model.build.BuildStrategyType;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.IGitBuildSource;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

@RunWith(MockitoJUnitRunner.class)
public class BuildConfigPropertySourceTest {
	
	@Mock private IBuildConfig resource;
	private BuildConfigPropertySource source;
	
	@Before
	public void setup(){
		Map<String, String> labels = new HashMap<>();
		labels.put("foo","bar");
		Map<String, String> annotations = new HashMap<>();
		annotations.put("xyz", "abc");
		annotations.put("efg", "def");
		
		when(resource.getOutputRepositoryName()).thenReturn("outputrepo");
		when(resource.getSourceURI()).thenReturn("git://foo.bar");
		givenGitBuildSource();
		givenSTIBuildStrategy();
		source = new BuildConfigPropertySource(resource);
	}
	private IDockerBuildStrategy givenDockerbuBuildStrategy(){
		IDockerBuildStrategy strategy = mock(IDockerBuildStrategy.class);
		when(strategy.getBaseImage()).thenReturn(new DockerImageURI("foobar"));
		when(strategy.getType()).thenReturn(BuildStrategyType.DOCKER);
		when(strategy.getContextDir()).thenReturn("thepathway");
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	private ISourceBuildStrategy givenSTIBuildStrategy(){
		ISourceBuildStrategy strategy  = mock(ISourceBuildStrategy.class);
		when(strategy.getType()).thenReturn(BuildStrategyType.SOURCE);
		when(strategy.getScriptsLocation()).thenReturn("scriptlocation");
		when(strategy.getImage()).thenReturn(new DockerImageURI("foobar"));
		Map<String, String> env = new HashMap<>();
		env.put("foo", "bar");
		when(strategy.getEnvironmentVariables()).thenReturn(env);
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	private ICustomBuildStrategy givenCustomBuildStrategy(){
		ICustomBuildStrategy strategy = mock(ICustomBuildStrategy.class);
		when(strategy.getType()).thenReturn(BuildStrategyType.CUSTOM);
		Map<String, String> env = new HashMap<>();
		env.put("foo", "bar");
		when(strategy.getEnvironmentVariables()).thenReturn(env);
		when(strategy.exposeDockerSocket()).thenReturn(true);
		when(strategy.getImage()).thenReturn(new DockerImageURI("foo/bar:latest"));
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	private IGitBuildSource givenGitBuildSource(){
		IGitBuildSource source = mock(IGitBuildSource.class);
		when(source.getType()).thenReturn(BuildSourceType.GIT);
		when(source.getRef()).thenReturn("altbranch");
		when(source.getURI()).thenReturn("git://foo.bar");
		when(resource.getBuildSource()).thenReturn(source);
		return source;
	}
	
	@Test
	public void getOutputPropertyValues(){
		assertEquals("outputrepo", resource.getOutputRepositoryName());
	}
	@Test
	public void getSTIPropertyValues(){
		ISourceBuildStrategy strategy = givenSTIBuildStrategy();
		assertEquals(BuildStrategyType.SOURCE, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
		assertEquals(strategy.getScriptsLocation(),  source.getPropertyValue(BuildConfigPropertySource.Ids.STI_SCRIPT_LOCATION));
		assertEquals(strategy.getImage(), source.getPropertyValue(BuildConfigPropertySource.Ids.STI_IMAGE));
		assertEquals(new KeyValuePropertySource(strategy.getEnvironmentVariables()), source.getPropertyValue(BuildConfigPropertySource.Ids.STI_ENV));
	}
	
	@Test
	public void getDockerPropertyValues(){
		IDockerBuildStrategy strategy = givenDockerbuBuildStrategy();
		assertEquals(BuildStrategyType.DOCKER, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
		assertEquals(strategy.getBaseImage(), source.getPropertyValue(BuildConfigPropertySource.Ids.DOCKER_IMAGE));
		assertEquals(strategy.getContextDir(), source.getPropertyValue(BuildConfigPropertySource.Ids.DOCKER_CONTEXT_DIR));
	}
	
	@Test
	public void getPropertyDescriptorForDockerBuild() {
		givenDockerbuBuildStrategy();

		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.Type, "Type", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.DOCKER_CONTEXT_DIR, "Context Dir", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.DOCKER_IMAGE, "Image", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.OUTPUT_REPO_NAME, "Image Stream Name", "Output")
		};
		assertPropertyDescriptorsContains(exp, source.getResourcePropertyDescriptors());
	}
	
	@Test
	public void getPropertyDescriptorForSTIBuild() {
		givenSTIBuildStrategy();
		
		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.Type, "Type", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.STI_SCRIPT_LOCATION, "Script Location", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.STI_IMAGE, "Image", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.STI_ENV, "Environment Variables", "Strategy")
		};
		assertPropertyDescriptorsContains(exp, source.getResourcePropertyDescriptors());
	}
	
	@Test
	public void getCustomPropertyValues(){
		ICustomBuildStrategy strategy = givenCustomBuildStrategy();
		assertEquals(BuildStrategyType.CUSTOM, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
		assertEquals(strategy.getImage(), source.getPropertyValue(BuildConfigPropertySource.Ids.CUSTOM_IMAGE));
		assertEquals(strategy.exposeDockerSocket(), source.getPropertyValue(BuildConfigPropertySource.Ids.CUSTOM_EXPOSE_DOCKER_SOCKET));
		assertEquals(new KeyValuePropertySource(strategy.getEnvironmentVariables()), source.getPropertyValue(BuildConfigPropertySource.Ids.CUSTOM_ENV));
	}
	
	@Test
	public void getPropertyDescriptorForCustomBuild() {
		givenCustomBuildStrategy();
		
		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.Type, "Type", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.CUSTOM_EXPOSE_DOCKER_SOCKET, "Expose Docker Socket", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.CUSTOM_IMAGE, "Image", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.CUSTOM_ENV, "Environment Variables", "Strategy")
		};
		assertPropertyDescriptorsContains(exp, source.getResourcePropertyDescriptors());
	}
	
	@Test
	public void getGitSourcePropertyValues(){
		IGitBuildSource buildSource = givenGitBuildSource();
		assertEquals(BuildSourceType.GIT, source.getPropertyValue(BuildConfigPropertySource.Ids.SOURCE_TYPE));
		assertEquals(buildSource.getRef(), source.getPropertyValue(BuildConfigPropertySource.Ids.SOURCE_GIT_REF));
		assertEquals(buildSource.getURI(), source.getPropertyValue(BuildConfigPropertySource.Ids.SOURCE_URI));
	}
	
	@Test
	public void getPropertyDescriptorForGitBuildSource(){
		givenGitBuildSource();
		
		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.SOURCE_TYPE, "Type", "Source"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.SOURCE_GIT_REF, "Ref", "Source"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.SOURCE_URI, "URI", "Source")
		};
		assertPropertyDescriptorsContains(exp, source.getResourcePropertyDescriptors());
		
	}
}
