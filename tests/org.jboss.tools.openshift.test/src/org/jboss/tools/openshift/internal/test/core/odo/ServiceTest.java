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
import org.jboss.tools.openshift.core.odo.Service;
import org.jboss.tools.openshift.core.odo.ServiceDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ServiceTest {
	private static URL srcUrl;

	private static ObjectMapper MAPPER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(List.class, new ServiceDeserializer());
		MAPPER.registerModule(module);
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/service-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = ServiceTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return ServiceTest.class.getResource("/resources" + urlString);
		}
		return url;
	}

	@Test
	public void verifyThatServicesCanLoad() throws IOException {
		List<Service> services = MAPPER.readValue(srcUrl, new TypeReference<List<Service>>() {
		});
		Assert.assertNotNull(services);
	}

	@Test
	public void verifyThatServicesReturnsItems() throws IOException {
		List<Service> services = MAPPER.readValue(srcUrl, new TypeReference<List<Service>>() {
		});
		Assert.assertNotNull(services);
		Assert.assertEquals(1, services.size());
		Assert.assertNotNull(services.get(0));
	}

	@Test
	public void verifyThatServicesReturnsName() throws IOException {
		List<Service> services = MAPPER.readValue(srcUrl, new TypeReference<List<Service>>() {
		});
		Assert.assertNotNull(services);
		Assert.assertEquals(1, services.size());
		Service service = services.get(0);
		Assert.assertNotNull(service);
		Assert.assertEquals("my-cluster", service.getName());
	}
}