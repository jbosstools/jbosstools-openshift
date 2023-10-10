/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.test.core.odo;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentDescriptorsDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ComponentDescriptorTest {
	private static URL srcUrl;

	private static ObjectMapper MAPPER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(List.class, new ComponentDescriptorsDeserializer("."));
		MAPPER.registerModule(module);
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/component-descriptor-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = ComponentDescriptorTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return ComponentDescriptorTest.class.getResource("/resources" + urlString);
		}
		return url;
	}

	@Test
	public void verifyThatComponentDescriptorsCanLoad() throws IOException {
		List<ComponentDescriptor> descriptors = MAPPER.readValue(srcUrl,
				new TypeReference<List<ComponentDescriptor>>() {
				});
		Assert.assertNotNull(descriptors);
	}

	@Test
	public void verifyThatComponentDescriptorsReturnsComponentDescriptor() throws IOException {
		List<ComponentDescriptor> descriptors = MAPPER.readValue(srcUrl,
				new TypeReference<List<ComponentDescriptor>>() {
				});
		Assert.assertNotNull(descriptors);
		Assert.assertEquals(1, descriptors.size());
		Assert.assertNotNull(descriptors.get(0));
	}

	@Test
	public void verifyThatComponentDescriptorsReturnsComponentDescriptorProperties() throws IOException {
		List<ComponentDescriptor> descriptors = MAPPER.readValue(srcUrl,
				new TypeReference<List<ComponentDescriptor>>() {
				});
		Assert.assertNotNull(descriptors);
		Assert.assertEquals(1, descriptors.size());
		ComponentDescriptor descriptor = descriptors.get(0);
		Assert.assertNotNull(descriptor);
		Assert.assertEquals("devcomp", descriptor.getName());
	}
}
