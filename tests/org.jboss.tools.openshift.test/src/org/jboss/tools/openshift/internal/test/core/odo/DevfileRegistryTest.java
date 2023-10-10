/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
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
import org.jboss.tools.openshift.core.odo.DevfileRegistriesDeserializer;
import org.jboss.tools.openshift.core.odo.DevfileRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class DevfileRegistryTest {
	private static URL srcUrl;

	private static ObjectMapper MAPPER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(List.class, new DevfileRegistriesDeserializer());
		MAPPER.registerModule(module);
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/devfile-registry-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = DevfileRegistryTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return DevfileRegistryTest.class.getResource("/resources" + urlString);
		}
		return url;
	}

	@Test
	public void verifyThatDevfileRegistriesCanLoad() throws IOException {
		List<DevfileRegistry> registries = MAPPER.readValue(srcUrl, new TypeReference<List<DevfileRegistry>>() {
		});
		Assert.assertNotNull(registries);
	}

	@Test
	public void verifyThatDevfileRegsitriesReturnsDevfileRegistry() throws IOException {
		List<DevfileRegistry> registries = MAPPER.readValue(srcUrl, new TypeReference<List<DevfileRegistry>>() {
		});
		Assert.assertNotNull(registries);
		Assert.assertEquals(1, registries.size());
		Assert.assertNotNull(registries.get(0));
	}

	@Test
	public void verifyThatDevfileRegsitriesReturnsDevfileRegistryProperties() throws IOException {
		List<DevfileRegistry> registries = MAPPER.readValue(srcUrl, new TypeReference<List<DevfileRegistry>>() {
		});
		Assert.assertNotNull(registries);
		Assert.assertEquals(1, registries.size());
		DevfileRegistry registry = registries.get(0);
		Assert.assertNotNull(registry);
		Assert.assertEquals("DefaultDevfileRegistry", registry.getName());
		Assert.assertEquals("https://registry.devfile.io", registry.getURL());
		Assert.assertEquals(true, registry.isSecure());
	}
}
