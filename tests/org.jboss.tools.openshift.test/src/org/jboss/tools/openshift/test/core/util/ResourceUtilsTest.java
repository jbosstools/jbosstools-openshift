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

import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.containsAll;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.getServicesForPod;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.imageRef;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isBuildPod;
import static org.jboss.tools.openshift.internal.core.util.ResourceUtils.isMatching;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
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

@RunWith(MockitoJUnitRunner.class)
public class ResourceUtilsTest {
	
	private static final String IMAGE_REF = "foo:latest";
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
}
