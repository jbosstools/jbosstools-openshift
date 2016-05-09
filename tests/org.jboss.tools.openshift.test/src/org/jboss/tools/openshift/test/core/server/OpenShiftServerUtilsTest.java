/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.eclipse.wst.server.core.IServerAttributes;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.junit.Test;

import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.build.IBuildStrategy;
import com.openshift.restclient.model.build.ICustomBuildStrategy;
import com.openshift.restclient.model.build.IDockerBuildStrategy;
import com.openshift.restclient.model.build.ISTIBuildStrategy;
import com.openshift.restclient.model.build.ISourceBuildStrategy;

public class OpenShiftServerUtilsTest {

	@Test
	public void testIsEapStyle() {
		assertIsNotEapStyle(null);
		//docker
		assertIsNotEapStyle(createBuildConfig(IDockerBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(IDockerBuildStrategy.class, "foo.wildflybar"));
		//source
		assertIsNotEapStyle(createBuildConfig(ISourceBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ISourceBuildStrategy.class, "foo.bar.eap70"));
		//custom source
		assertIsNotEapStyle(createBuildConfig(ICustomBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ICustomBuildStrategy.class, "foo.bar.EAP64"));
		//deprecated STI
		assertIsNotEapStyle(createBuildConfig(ISTIBuildStrategy.class, "foo.bar"));
		assertIsEapStyle(createBuildConfig(ISTIBuildStrategy.class, "wildflyyy"));
		
		//fallback on template name check
		assertIsNotEapStyle(createBuildConfig(null, "foo.bar"));
		assertIsEapStyle(createBuildConfig(IBuildStrategy.class, "wildflyyy"));

	}

	@Test
	public void testContainsEap2LikeKeywords() {
		assertNotContainsEapLikeKeywords(null);
		assertNotContainsEapLikeKeywords("");
		assertContainsEapLikeKeywords("jboss-eap64");
		assertContainsEapLikeKeywords("mixed.wildFly.case");
	}

	private IBuildConfig createBuildConfig(Class<? extends IBuildStrategy> clazz, String name) {
		IBuildConfig bc = mock(IBuildConfig.class);
		DockerImageURI image = mock(DockerImageURI.class);
		when(image.getName()).thenReturn(name);
		IBuildStrategy strategy = null;
		if (clazz == null) {
			strategy = mock(ISourceBuildStrategy.class);
		} else if (IDockerBuildStrategy.class.isAssignableFrom(clazz)) {
			IDockerBuildStrategy dbs = mock(IDockerBuildStrategy.class);
			when(dbs.getBaseImage()).thenReturn(image);
			strategy = dbs;
		} else if (ICustomBuildStrategy.class.isAssignableFrom(clazz)) {
			ICustomBuildStrategy cbs = mock(ICustomBuildStrategy.class);
			when(cbs.getImage()).thenReturn(image);
			strategy = cbs;
		} else if (ISTIBuildStrategy.class.isAssignableFrom(clazz)) {
			ISTIBuildStrategy sts = mock(ISTIBuildStrategy.class);
			when(sts.getImage()).thenReturn(image);
			strategy = sts;
		}  else if (ISourceBuildStrategy.class.isAssignableFrom(clazz)) {
			ISourceBuildStrategy sbs = mock(ISourceBuildStrategy.class);
			when(sbs.getImage()).thenReturn(image);
			strategy = sbs;
		}
		when(bc.getBuildStrategy()).thenReturn(strategy);
		
		Map<String,String> labels = Collections.singletonMap("template", name);
		when(bc.getLabels()).thenReturn(labels);
		
		return bc;
	}
	
	private void assertIsEapStyle(IBuildConfig buildConfig) {
		assertTrue(OpenShiftServerUtils.isEapStyle(buildConfig));
	}

	private void assertIsNotEapStyle(IBuildConfig buildConfig) {
		assertFalse(OpenShiftServerUtils.isEapStyle(buildConfig));
	}
	
	private void assertContainsEapLikeKeywords(String text) {
		assertTrue(OpenShiftServerUtils.containsEapLikeKeywords(text));
	}
	
	private void assertNotContainsEapLikeKeywords(String text) {
		assertFalse(OpenShiftServerUtils.containsEapLikeKeywords(text));
	}
	
	
	@Test
	public void testGetPodPathFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_POD_PATH, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getPodPath(server));
	}
	@Test
	public void testGetSourceFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_SOURCE_PATH, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getSourcePath(server));
	}
	@Test
	public void testGetRouteURLFromServer() {
		// Only tests resolution from server
		IServerAttributes server = mock(IServerAttributes.class);
		when(server.getAttribute(OpenShiftServerUtils.ATTR_ROUTE, (String)null)).thenReturn("test1");
		assertEquals("test1", OpenShiftServerUtils.getRouteURL(server));
	}
}
