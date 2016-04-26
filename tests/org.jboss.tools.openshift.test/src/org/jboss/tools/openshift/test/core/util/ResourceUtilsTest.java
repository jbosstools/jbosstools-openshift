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
package org.jboss.tools.openshift.test.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.areRelated;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.containsAll;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getBuildConfigForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getBuildConfigsForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getRouteForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getRoutesForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getServicesForPod;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.imageRef;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isBuildPod;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IObjectReference;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.deploy.IDeploymentImageChangeTrigger;
import com.openshift.restclient.model.route.IRoute;

@RunWith(MockitoJUnitRunner.class)
public class ResourceUtilsTest {
	
	private static final String IMAGE_REF = "foo:latest";
	
	private static final IService SERVICE_42 =
			ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("42"));
	private static final IBuildConfig[] BUILDCONFIGS = new IBuildConfig[] {
			ResourceMocks.createResource(IBuildConfig.class, config -> when(config.getName()).thenReturn("41")),
			ResourceMocks.createResource(IBuildConfig.class, config -> when(config.getName()).thenReturn("42")),
			ResourceMocks.createResource(IBuildConfig.class, config -> when(config.getName()).thenReturn("42a")),
			ResourceMocks.createResource(IBuildConfig.class, config -> when(config.getName()).thenReturn("42")),
			ResourceMocks.createResource(IBuildConfig.class, config -> when(config.getName()).thenReturn("a42a")) 
	};
	private static final IRoute[] ROUTES = new IRoute[] {
			ResourceMocks.createResource(IRoute.class, config -> when(config.getServiceName()).thenReturn("41")),
			ResourceMocks.createResource(IRoute.class, config -> when(config.getServiceName()).thenReturn("42")),
			ResourceMocks.createResource(IRoute.class, config -> when(config.getServiceName()).thenReturn("42a")),
			ResourceMocks.createResource(IRoute.class, config -> when(config.getServiceName()).thenReturn("42")),
			ResourceMocks.createResource(IRoute.class, config -> when(config.getServiceName()).thenReturn("a42a")) 
	};

	private Map<String, String> podLabels = new HashMap<>();
	private Map<String, String> serviceSelector = new HashMap<>();
	@Mock private IPod pod;
	@Mock private IBuildConfig buildConfig;
	@Mock private IBuild build;
	@Mock private IDeploymentImageChangeTrigger deployTrigger;
	@Mock private IObjectReference objectRef;
	
	@Before
	public void setup() {
		podLabels.put("foo", "bar");
		podLabels.put("xyz", "bar");	
		serviceSelector.put("foo", "bar");
		
		when(buildConfig.getBuildOutputReference()).thenReturn(objectRef);
		when(objectRef.getName()).thenReturn(IMAGE_REF);
	}
	
	@Test
	public void testContainsAllDoesNotNullPointer() {
		assertFalse(containsAll(null, new HashMap<>()));
		assertFalse(containsAll(new HashMap<>(), null));
	}
	
	@Test
	public void testContainsAllDoNotMatchWhenTargetDoesNotContainAllSourceKeys() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target = new HashMap<>(); 
		target.put("xyz", "abc");
		
		assertFalse(containsAll(source, target));
	}
	
	@Test
	public void testContainsAllDoNotMatchWhenTargetValuesDoNotMatchSourceValues() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target = new HashMap<>(); 
		target.put("foo", "abc");
		target.put("xyz", "bar");
		
		assertFalse(containsAll(source, target));
	}
	
	@Test
	public void testContainsAllMatchesWhenTargetIncludesAllSourceKeyAndValues() {
		Map<String, String> source = new HashMap<>(); 
		source.put("foo", "bar");
		Map<String, String> target= new HashMap<>(); 
		target.put("foo", "bar");
		target.put("xyz", "bar");
		assertTrue(containsAll(source, target));
	}

	@Test
	public void testGetServicesForPod() {
		when(pod.getLabels()).thenReturn(podLabels);
		
		IService match = mock(IService.class);
		when(match.getSelector()).thenReturn(serviceSelector);
		IService nomatch = mock(IService.class);
		when(nomatch.getSelector()).thenReturn(new HashMap<>());
		
		Collection<IService> services = Arrays.asList(nomatch,match);
		IService [] exp = new IService[] {match};
		assertArrayEquals(exp, getServicesForPod(pod, services).toArray());
	}
	
	@Test
	public void testIsBuildPodWhenHasBuildAnnotation() {
		when(pod.isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME)).thenReturn(true);
		assertTrue(isBuildPod(pod));
	}
	@Test
	public void testIsBuildPodWhenDoesNotHaveBuildAnnotation() {
		when(pod.isAnnotatedWith(OpenShiftAPIAnnotations.BUILD_NAME)).thenReturn(false);
		assertFalse(isBuildPod(pod));
	}
	
	@Test
	public void testImageRefForBuildConfig() {
		assertEquals("", imageRef((IBuildConfig)null));

		when(objectRef.getKind()).thenReturn("something");
		assertEquals("", imageRef(buildConfig));
		
		when(objectRef.getKind()).thenReturn(ResourceKind.IMAGE_STREAM_TAG);
		assertEquals(IMAGE_REF, imageRef(buildConfig));
		
		when(objectRef.getKind()).thenReturn(ResourceUtils.IMAGE_STREAM_IMAGE_KIND);
		assertEquals(IMAGE_REF, imageRef(buildConfig));
	}
	
	@Test
	public void testImageRefForDeploymentImageChangeTrigger() {
		assertEquals("", imageRef((IDeploymentImageChangeTrigger)null));
		
		when(deployTrigger.getKind()).thenReturn("something");
		assertEquals("", imageRef(deployTrigger));

		DockerImageURI uri = new DockerImageURI(IMAGE_REF);
		when(deployTrigger.getFrom()).thenReturn(uri);
		
		when(deployTrigger.getKind()).thenReturn(ResourceKind.IMAGE_STREAM_TAG);
		assertEquals(IMAGE_REF, imageRef(deployTrigger));
		
		when(deployTrigger.getKind()).thenReturn(ResourceUtils.IMAGE_STREAM_IMAGE_KIND);
		assertEquals(IMAGE_REF, imageRef(deployTrigger));
		
		when(deployTrigger.getKind()).thenReturn(ResourceUtils.DOCKER_IMAGE_KIND);
		assertEquals(IMAGE_REF, imageRef(deployTrigger));
	}

	@Test
	public void testImageRefForBuild() {
		assertEquals("", imageRef((IBuild)null));
		
		when(build.getOutputKind()).thenReturn("something");
		assertEquals("", imageRef(build));
		
		DockerImageURI uri = new DockerImageURI(IMAGE_REF);
		when(build.getOutputTo()).thenReturn(uri);
		
		when(build.getOutputKind()).thenReturn(ResourceKind.IMAGE_STREAM_TAG);
		assertEquals(IMAGE_REF, imageRef(build));
		
		when(build.getOutputKind()).thenReturn(ResourceUtils.IMAGE_STREAM_IMAGE_KIND);
		assertEquals(IMAGE_REF, imageRef(build));
		
		when(build.getOutputKind()).thenReturn(ResourceUtils.DOCKER_IMAGE_KIND);
		assertEquals(IMAGE_REF, imageRef(build));
	}
	
	@Test
	public void testAreRelatedForBuildConfigAndService() {
		// given
		// when
		// then
		assertThat(areRelated((IBuildConfig) null, (IService) null)).isFalse();

		// given
		// when
		// then
		assertThat(areRelated(mock(IBuildConfig.class), (IService) null)).isFalse();

		// given
		// when
		// then
		assertThat(areRelated((IBuildConfig) null, mock(IService.class))).isFalse();

		// given
		IBuildConfig buildConfig = mock(IBuildConfig.class);
		when(buildConfig.getName()).thenReturn("42");
		IService service = mock(IService.class);
		when(service.getName()).thenReturn("24");
		// when
		// then
		assertThat(areRelated(buildConfig, service)).isFalse();
		
		// given
		buildConfig = mock(IBuildConfig.class);
		when(buildConfig.getName()).thenReturn("42");
		service = mock(IService.class);
		when(service.getName()).thenReturn("42");
		// when
		// then
		assertThat(areRelated(buildConfig, service)).isTrue();
	}

	@Test
	public void testGetBuildConfigsForService() {
		// given
		// when
		List<IBuildConfig> matchingConfigs = getBuildConfigsForService(SERVICE_42, Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfigs).containsExactly(BUILDCONFIGS[1], BUILDCONFIGS[3]);

		// when
		matchingConfigs = getBuildConfigsForService(SERVICE_42, null);
		// then
		assertThat(matchingConfigs).isEmpty();
		
		// when
		matchingConfigs = getBuildConfigsForService((IService) null, Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfigs).isEmpty();

		// when
		matchingConfigs = getBuildConfigsForService(
				ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("0")), 
				Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfigs).isEmpty();
	}

	@Test
	public void testGetBuildConfigForService() {
		// given
		// when
		IBuildConfig matchingConfig = getBuildConfigForService(SERVICE_42, Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfig).isEqualTo(BUILDCONFIGS[1]);

		// when
		matchingConfig = getBuildConfigForService(SERVICE_42, null);
		// then
		assertThat(matchingConfig).isNull();
		
		// when
		matchingConfig = getBuildConfigForService((IService) null, Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfig).isNull();

		// when
		matchingConfig = getBuildConfigForService(
				ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("0")), 
				Arrays.asList(BUILDCONFIGS));
		// then
		assertThat(matchingConfig).isNull();
	}

	@Test
	public void testAreRelatedForRouteAndService() {
		// given
		// when
		// then
		assertThat(areRelated((IRoute) null, (IService) null)).isFalse();

		// given
		// when
		// then
		assertThat(areRelated(mock(IRoute.class), (IService) null)).isFalse();

		// given
		// when
		// then
		assertThat(areRelated((IRoute) null, mock(IService.class))).isFalse();

		// given
		IRoute route = mock(IRoute.class);
		when(route.getServiceName()).thenReturn("42");
		IService service = mock(IService.class);
		when(service.getName()).thenReturn("24");
		// when
		// then
		assertThat(areRelated(route, service)).isFalse();
		
		// given
		route = mock(IRoute.class);
		when(route.getServiceName()).thenReturn("42");
		service = mock(IService.class);
		when(service.getName()).thenReturn("42");
		// when
		// then
		assertThat(areRelated(route, service)).isTrue();
	}

	@Test
	public void testGetRoutesForService() {
		// given
		// when
		List<IRoute> routes = getRoutesForService(SERVICE_42, Arrays.asList(ROUTES));
		// then
		assertThat(routes).containsExactly(ROUTES[1], ROUTES[3]);

		// when
		routes = getRoutesForService(SERVICE_42, null);
		// then
		assertThat(routes).isEmpty();
		
		// when
		routes = getRoutesForService((IService) null, Arrays.asList(ROUTES));
		// then
		assertThat(routes).isEmpty();

		// when
		routes = getRoutesForService(
				ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("0")), 
				Arrays.asList(ROUTES));
		// then
		assertThat(routes).isEmpty();
	}

	@Test
	public void testGetRouteForService() {
		// given
		// when
		IRoute route = getRouteForService(SERVICE_42, Arrays.asList(ROUTES));
		// then
		assertThat(route).isEqualTo(ROUTES[1]);

		// when
		route = getRouteForService(SERVICE_42, null);
		// then
		assertThat(route).isNull();
		
		// when
		route = getRouteForService((IService) null, Arrays.asList(ROUTES));
		// then
		assertThat(route).isNull();

		// when
		route = getRouteForService(
				ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("0")), 
				Arrays.asList(ROUTES));
		// then
		assertThat(route).isNull();
	}

	
}
