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
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getDeploymentConfigNameForPods;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getImageRefs;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getRouteForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getRoutesForService;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getServicesForPod;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.imageRef;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isBuildPod;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isMatching;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
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
	@Mock private ITags tagsCap;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		podLabels.put("foo", "bar");
		podLabels.put("xyz", "bar");

		when(pod.getLabels()).thenReturn(podLabels);
		
		serviceSelector.put("foo", "bar");
		
		when(buildConfig.getBuildOutputReference()).thenReturn(objectRef);
		when(objectRef.getName()).thenReturn(IMAGE_REF);
		when(build.getName()).thenReturn("build");
		when(build.accept(any(CapabilityVisitor.class), anyBoolean()))
			.then(new Answer<Boolean>() {

				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					CapabilityVisitor<ITags, Boolean> vis = (CapabilityVisitor<ITags, Boolean>) invocation.getArguments()[0];
					return vis.visit(tagsCap);
				}
				
			});
		when(tagsCap.getTags()).thenReturn(Collections.emptyList());
	}
	
	@Test
	public void testIsMatchingWhenFilterIsBlank() {
		assertTrue("Exp. a match on blank string", isMatching("   ","something", Collections.emptyList()));
	}
	
	@Test
	public void testIsMatchingResourceWhenFilterIsBlank() {
		assertTrue("Exp. a match on blank string", isMatching("   ", build));
	}

	@Test
	public void testIsMatchingWhenNameIsInFilterTextThatIsDelimited() {
		assertTrue("Exp. a match on delimited filter", isMatching("java,jruby","torque-java-jruby", Collections.emptyList()));
	}
	
	@Test
	public void testIsMatchingResourceWhenNameIsInFilterTextThatIsDelimited() {
		when(build.getName()).thenReturn("torque-java-jruby");
		assertTrue("Exp. a match on delimited filter", isMatching("java,jruby",build));
	}
	
	@Test
	public void testIsMatchingWhenNameIsInFilterTextThatIsNotDelimited() {
		assertTrue("Exp. a match on undelimited filter", isMatching(" bar ","bar", Collections.emptyList()));
	}
	
	@Test
	public void testIsMatchingResourceWhenNameIsInFilterTextThatIsNotDelimited() {
		when(build.getName()).thenReturn("bar");
		assertTrue("Exp. a match on undelimited filter", isMatching(" bar ",build));
	}

	@Test
	public void testIsMatchingWhenNameMatchesTags() {
		assertTrue("Exp. a match on a tag", isMatching("bar","barrr", Arrays.asList("xyz","bar")));
	}

	@Test
	public void testIsMatchingResourceWhenNameMatchesTags() {
		when(build.getName()).thenReturn("barrr");
		when(tagsCap.getTags()).thenReturn(Arrays.asList("xyz","bar"));
		assertTrue("Exp. a match on a tag", isMatching("bar",build));
	}
	
	@Test
	public void testIsNotMatchingWhenNameIsInFilterTextIsNotNameOrInTag() {
		assertFalse("Exp. no match", isMatching("foo","bar", Arrays.asList("xyz","123")));
	}

	@Test
	public void testIsNotMatchingResourceWhenNameIsInFilterTextIsNotNameOrInTag() {
		when(build.getName()).thenReturn("bar");
		when(tagsCap.getTags()).thenReturn(Arrays.asList("xyz","123"));
		assertFalse("Exp. no match", isMatching("foo","bar", Arrays.asList("xyz","123")));
	}

	@Test
	public void testIsMatchingForNullResourceIsFalse() {
		assertTrue(isMatching("text", null));
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
	public void nullBuildConfigListShouldReturnNullImageRefsList() {
		// given
		// when
		List<String> imageRefs = getImageRefs(null);
		// then
		assertThat(imageRefs).isNull();
	}

	@Test
	public void emptyBuildConfigListShouldReturnEmptyImageRefsList() {
		// given
		List<IBuildConfig> buildConfigs = new ArrayList<IBuildConfig>();
		// when
		List<String> imageRefs = getImageRefs(buildConfigs);
		// then
		assertThat(imageRefs).isEmpty();
	}
	
	@Test
	public void buildConfigListWithNullEntryShouldReturnEmptyStringImageRef() {
		// given
		// when
		List<String> imageRefs = getImageRefs(Arrays.asList((IBuildConfig) null));
		// then
		assertThat(imageRefs).containsExactly("");
	}

	@Test
	public void buildConfigsListWithErronousKindsShouldReturnEmptyStringAndValidImageRefs() {
		// given
		List<IBuildConfig> buildConfigs = Arrays.asList(
				ResourceMocks.createBuildConfig(null, "nullKind"),
				ResourceMocks.createBuildConfig(ResourceKind.IMAGE_STREAM_TAG, "imageStreamTagKind"),
				ResourceMocks.createBuildConfig("foo", "fooKind"),
				ResourceMocks.createBuildConfig(ResourceUtils.IMAGE_STREAM_IMAGE_KIND, "imageStreamImageKind"));
		// when
		List<String> imageRefs = getImageRefs(buildConfigs);
		// then
		assertThat(imageRefs).containsOnly("", "", "imageStreamTagKind", "imageStreamImageKind");
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
	public void NullRouteAndNullServiceShouldNotBeRelated() {
		// given
		// when
		// then
		assertThat(areRelated((IRoute) null, (IService) null)).isFalse();
	}

	public void routeShouldNotBerelatedToNullService() {
		// given
		// when
		// then
		assertThat(areRelated(mock(IRoute.class), (IService) null)).isFalse();
	}
	
	public void serviceShouldNotBeRelatedToNullRoute() {
		// given
		// when
		// then
		assertThat(areRelated((IRoute) null, mock(IService.class))).isFalse();
	}

	public void serviceAndRouteWithDifferentNameShouldNotBeRelated() {
		// given
		IRoute route = mock(IRoute.class);
		when(route.getServiceName()).thenReturn("42");
		IService service = mock(IService.class);
		when(service.getName()).thenReturn("24");
		// when
		// then
		assertThat(areRelated(route, service)).isFalse();
	}

	public void serviceAndRouteWithSameNameShouldBeRelated() {
		// given
		IRoute route = mock(IRoute.class);
		when(route.getServiceName()).thenReturn("42");
		IService service = mock(IService.class);
		when(service.getName()).thenReturn("42");
		// when
		// then
		assertThat(areRelated(route, service)).isTrue();
	}

	@Test
	public void routesWithServiceNameThatMatchServiceByNameShouldGetReturned() {
		// given
		// when
		List<IRoute> routes = getRoutesForService(SERVICE_42, Arrays.asList(ROUTES));
		// then
		assertThat(routes).containsExactly(ROUTES[1], ROUTES[3]);
	}
	
	@Test
	public void serviceShouldNotMatchNullRoutes() {
		// when
		List<IRoute> routes = getRoutesForService(SERVICE_42, null);
		// then
		assertThat(routes).isEmpty();
	}

	@Test
	public void routesShouldNotMatchNullService() {
		// when
		List<IRoute> routes = getRoutesForService((IService) null, Arrays.asList(ROUTES));
		// then
		assertThat(routes).isEmpty();
	}

	@Test
	public void testGetRoutesForService() {
		// when
		List<IRoute> routes = getRoutesForService(
				ResourceMocks.createResource(IService.class, config -> when(config.getName()).thenReturn("0")), 
				Arrays.asList(ROUTES));
		// then
		assertThat(routes).isEmpty();
	}

	@Test
	public void routeServiceNameShouldMatchServiceInName() {
		// given
		// when
		IRoute route = getRouteForService(SERVICE_42, Arrays.asList(ROUTES));
		// then
		assertThat(route).isEqualTo(ROUTES[1]);
	}
	
	@Test
	public void nullRouteShouldBeReturnedIfNullRouteIsGiven() {
		// when
		IRoute route = getRouteForService(SERVICE_42, null);
		// then
		assertThat(route).isNull();
	}

	@Test
	public void nullRouteShouldBeReturnedIfNullServiceIsGiven() {
		// when
		IRoute route = getRouteForService((IService) null, Arrays.asList(ROUTES));
		// then
		assertThat(route).isNull();
	}

	@Test
	public void testGetRouteForService() {
		// when
		IRoute route = getRouteForService(
				ResourceMocks.createResource(IService.class, service -> when(service.getName()).thenReturn("0")), 
				Arrays.asList(ROUTES));
		// then
		assertThat(route).isNull();
	}

	@Test
	public void nullPodsShouldReturnNullDeploymentConfigName() {
		// given
		// when
		String name = getDeploymentConfigNameForPods(null);
		// then
		assertThat(name).isNull();
	}

	@Test
	public void emptyPodListShouldReturnNullDeploymentConfigName() {
		// given empty pod list
		// when
		String name = getDeploymentConfigNameForPods(Collections.emptyList());
		// then
		assertThat(name).isNull();
	}

	@Test
	public void podListWithoutDeploymentConfigKeyShouldReturnNullDeploymentConfigName() {
		// given
		List<IPod> pods = Arrays.asList(
				ResourceMocks.createResource(IPod.class),
				pod,
				ResourceMocks.createResource(IPod.class));
		// when
		String name = getDeploymentConfigNameForPods(pods);
		// then
		assertThat(name).isNull();
	}

	@Test
	public void podListWithDeploymentConfigKeyShouldReturnDeploymentConfigName() {
		// given 
		final HashMap<String, String> podLabels = new HashMap<>();
		podLabels.put("foo", "booh");
		podLabels.put("bar", "car");
		podLabels.put(ResourceUtils.DEPLOYMENT_CONFIG_KEY, "hooolahoo");
		IPod pod = ResourceMocks.createResource(IPod.class, p -> when(p.getLabels()).thenReturn(podLabels));
		List<IPod> pods = Arrays.asList(ResourceMocks.createResource(IPod.class),
				pod,
				ResourceMocks.createResource(IPod.class));
		// when
		String name = getDeploymentConfigNameForPods(pods);
		// then
		assertThat(name).isEqualTo("hooolahoo");
	}

	@Test
	public void podListWithSeveralDeploymentConfigKeyShouldReturnFirstDeploymentConfigName() {
		// given 2 pods with deployment-config-key label
		HashMap<String, String> podLabels1 = new HashMap<>();
		podLabels1.put("foo", "bar");
		podLabels1.put("bar", "car");
		podLabels1.put(ResourceUtils.DEPLOYMENT_CONFIG_KEY, "hooolahoo");
		IPod pod1 = ResourceMocks.createResource(IPod.class, p -> when(p.getLabels()).thenReturn(podLabels1));		

		final HashMap<String, String> podLabels2 = new HashMap<>();
		podLabels2.put("kung", "foo");
		podLabels2.put(ResourceUtils.DEPLOYMENT_CONFIG_KEY, "hookaboo");
		IPod pod2 = ResourceMocks.createResource(IPod.class, p -> when(p.getLabels()).thenReturn(podLabels2));		
		List<IPod> pods = Arrays.asList(ResourceMocks.createResource(IPod.class), pod1, pod2);
		// when
		String name = getDeploymentConfigNameForPods(pods);
		// then
		assertThat(name).isEqualTo("hooolahoo");
	}
	
}
