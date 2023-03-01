/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
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
import java.util.Collections;
import java.util.List;

import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersionList;

public class ServiceTemplateRequiredTest {
	private static URL srcUrl;

	private static ServiceTemplatesDeserializer DESERIALIZER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		GenericKubernetesResource kafkaKind = new GenericKubernetesResource();
		kafkaKind.setApiVersion("rds.services.k8s.aws/v1alpha1");
		kafkaKind.setKind("DBCluster");
		DESERIALIZER = new ServiceTemplatesDeserializer(s -> null, Collections.singletonList(kafkaKind));
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/service-template-required-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = ServiceTemplateRequiredTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return ServiceTemplateRequiredTest.class.getResource("/resources" + urlString);
		}
		return url;
	}

	@Test
	public void verifyThatServiceTemplatesCanLoad() throws IOException {
		List<ServiceTemplate> serviceTemplates = DESERIALIZER
				.fromList(Serialization.unmarshal(srcUrl.openStream(), ClusterServiceVersionList.class));
		Assert.assertNotNull(serviceTemplates);
	}

	@Test
	public void verifyThatServiceTemplatesReturnsItems() throws IOException {
		List<ServiceTemplate> serviceTemplates = DESERIALIZER
				.fromList(Serialization.unmarshal(srcUrl.openStream(), ClusterServiceVersionList.class));
		Assert.assertNotNull(serviceTemplates);
		Assert.assertEquals(1, serviceTemplates.size());
	}
}
