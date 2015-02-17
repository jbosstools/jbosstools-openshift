/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.property;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.jboss.tools.openshift.test.ui.property.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.openshift3.client.images.DockerImageURI;
import com.openshift3.client.model.IBuildConfig;
import com.openshift3.client.model.build.BuildStrategyType;
import com.openshift3.client.model.build.ICustomBuildStrategy;
import com.openshift3.client.model.build.IDockerBuildStrategy;
import com.openshift3.client.model.build.ISTIBuildStrategy;

import org.jboss.tools.openshift.internal.ui.property.BuildConfigPropertySource;
import org.jboss.tools.openshift.internal.ui.property.KeyValuePropertySource;
import org.jboss.tools.openshift.internal.ui.property.ExtTextPropertyDescriptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildConfigPropertySourceTest {
	
	@Mock private IBuildConfig resource;
	private BuildConfigPropertySource source;
	
	@Before
	public void setup(){
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("foo","bar");
		Map<String, String> annotations = new HashMap<String, String>();
		annotations.put("xyz", "abc");
		annotations.put("efg", "def");
		
		when(resource.getSourceURI()).thenReturn("git://foo.bar");
		source = new BuildConfigPropertySource(resource);
	}
	private IDockerBuildStrategy givenDockerbuBuildStrategy(){
		IDockerBuildStrategy strategy = mock(IDockerBuildStrategy.class);
		when(strategy.getBaseImage()).thenReturn(new DockerImageURI("foobar"));
		when(strategy.getType()).thenReturn(BuildStrategyType.Docker);
		when(strategy.getContextDir()).thenReturn("thepathway");
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	private ISTIBuildStrategy givenSTIBuildStrategy(){
		ISTIBuildStrategy strategy  = mock(ISTIBuildStrategy.class);
		when(strategy.getType()).thenReturn(BuildStrategyType.STI);
		when(strategy.getScriptsLocation()).thenReturn("scriptlocation");
		when(strategy.getImage()).thenReturn(new DockerImageURI("foobar"));
		Map<String, String> env = new HashMap<String, String>();
		env.put("foo", "bar");
		when(strategy.getEnvironmentVariables()).thenReturn(env);
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	private ICustomBuildStrategy givenCustomBuildStrategy(){
		ICustomBuildStrategy strategy = mock(ICustomBuildStrategy.class);
		when(strategy.getType()).thenReturn(BuildStrategyType.Custom);
		Map<String, String> env = new HashMap<String, String>();
		env.put("foo", "bar");
		when(strategy.getEnvironmentVariables()).thenReturn(env);
		when(strategy.exposeDockerSocket()).thenReturn(true);
		when(strategy.getImage()).thenReturn(new DockerImageURI("foo/bar:latest"));
		when(resource.getBuildStrategy()).thenReturn(strategy);
		return strategy;
	}
	
	@Test
	public void getSTIPropertyValues(){
		ISTIBuildStrategy strategy = givenSTIBuildStrategy();
		assertEquals(BuildStrategyType.STI, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
		assertEquals(strategy.getScriptsLocation(),  source.getPropertyValue(BuildConfigPropertySource.Ids.STI_SCRIPT_LOCATION));
		assertEquals(strategy.getImage(), source.getPropertyValue(BuildConfigPropertySource.Ids.STI_IMAGE));
		assertEquals(new KeyValuePropertySource(strategy.getEnvironmentVariables()), source.getPropertyValue(BuildConfigPropertySource.Ids.STI_ENV));
	}
	
	@Test
	public void getDockerPropertyValues(){
		IDockerBuildStrategy strategy = givenDockerbuBuildStrategy();
		assertEquals(BuildStrategyType.Docker, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
		assertEquals(strategy.getBaseImage(), source.getPropertyValue(BuildConfigPropertySource.Ids.DOCKER_IMAGE));
		assertEquals(strategy.getContextDir(), source.getPropertyValue(BuildConfigPropertySource.Ids.DOCKER_CONTEXT_DIR));
	}
	
	@Test
	public void getPropertyDescriptorForDockerBuild() {
		givenDockerbuBuildStrategy();

		IPropertyDescriptor [] exp = new IPropertyDescriptor[]{
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.Type, "Type", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.DOCKER_CONTEXT_DIR, "Context Dir", "Strategy"),
				new ExtTextPropertyDescriptor(BuildConfigPropertySource.Ids.DOCKER_IMAGE, "Image", "Strategy")
		};
		assertPropertyDescriptors(exp, source.getResourcePropertyDescriptors());
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
		assertPropertyDescriptors(exp, source.getResourcePropertyDescriptors());
	}
	
	@Test
	public void getCustomPropertyValues(){
		ICustomBuildStrategy strategy = givenCustomBuildStrategy();
		assertEquals(BuildStrategyType.Custom, source.getPropertyValue(BuildConfigPropertySource.Ids.Type));
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
		assertPropertyDescriptors(exp, source.getResourcePropertyDescriptors());
	}

}
