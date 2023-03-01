/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
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
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ComponentDeserializerTest {
	private static URL srcUrl;

	private static ObjectMapper MAPPER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(List.class, new ComponentDeserializer());
		MAPPER.registerModule(module);
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/components-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = ComponentDeserializerTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return ComponentDeserializerTest.class.getResource("/resources" + urlString);
		}
		return url;
	}
	
	@Test
	public void verifyThatComponentsCanLoad() throws IOException {
		List<Component> components = MAPPER.readValue(srcUrl, new TypeReference<List<Component>>() {
		});
		Assert.assertNotNull(components);
	}

	@Test
	public void verifyThatComponentDeserializerReturnsComponents() throws IOException {
		List<Component> components = MAPPER.readValue(srcUrl, new TypeReference<List<Component>>() {
		});
		Assert.assertNotNull(components);
		Assert.assertEquals(1, components.size());
		Assert.assertNotNull(components.get(0));
	}

	@Test
	public void verifyThatComponentDeserializerReturnsComponentsPropertiesForDevfileComponent() throws IOException {
		List<Component> components = MAPPER.readValue(srcUrl, new TypeReference<List<Component>>() {
		});
		Assert.assertNotNull(components);
		Assert.assertEquals(1, components.size());
		// Devfile components
		Component component = components.get(0);
		Assert.assertNotNull(component);
		Assert.assertEquals("nodejs1", component.getName());
	}

	@Test
	@Ignore
	public void verifyThatComponentDeserializerReturnsComponentsPropertiesForNonOdoComponent() throws IOException {
		List<Component> components = MAPPER.readValue(srcUrl, new TypeReference<List<Component>>() {
		});
		Assert.assertNotNull(components);
		Assert.assertEquals(2, components.size());
		// non odo components
		Component component = components.get(1);
		Assert.assertNotNull(component);
		Assert.assertEquals("quarkus1", component.getName());
	}
}