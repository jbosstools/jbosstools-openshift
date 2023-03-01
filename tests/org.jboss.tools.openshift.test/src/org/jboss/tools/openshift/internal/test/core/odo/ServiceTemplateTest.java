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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.OperatorCRDSpecDescriptor;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersionList;

public class ServiceTemplateTest {
	private static URL srcUrl;

	private static ServiceTemplatesDeserializer DESERIALIZER;

	private static boolean inEclipse;

	@BeforeClass
	public static void setup() {
		GenericKubernetesResource kafkaKind = new GenericKubernetesResource();
		kafkaKind.setApiVersion("kafka.strimzi.io/v1beta2");
		kafkaKind.setKind("Kafka");
		DESERIALIZER = new ServiceTemplatesDeserializer(s -> null, Collections.singletonList(kafkaKind));
		inEclipse = PlatformUI.isWorkbenchRunning();
		srcUrl = getURL("/service-template-test.json");
	}

	private static URL getURL(String urlString) {
		URL url = ServiceTemplateTest.class.getResource(urlString);
		if (url == null && inEclipse) {
			return ServiceTemplateTest.class.getResource("/resources" + urlString);
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
		Assert.assertNotNull(serviceTemplates.get(0));
	}

	@Test
	public void verifyThatServiceTemplatesReturnsName() throws IOException {
		List<ServiceTemplate> serviceTemplates = DESERIALIZER
				.fromList(Serialization.unmarshal(srcUrl.openStream(), ClusterServiceVersionList.class));
		Assert.assertNotNull(serviceTemplates);
		Assert.assertEquals(1, serviceTemplates.size());
		ServiceTemplate serviceTemplate = serviceTemplates.get(0);
		Assert.assertNotNull(serviceTemplate);
		Assert.assertEquals("strimzi-cluster-operator.v0.32.0", serviceTemplate.getName());
	}

	@Test
	public void verifyThatServiceTemplatesReturnsCRDs() throws IOException {
		List<ServiceTemplate> serviceTemplates = DESERIALIZER
				.fromList(Serialization.unmarshal(srcUrl.openStream(), ClusterServiceVersionList.class));
		Assert.assertNotNull(serviceTemplates);
		Assert.assertEquals(1, serviceTemplates.size());
		ServiceTemplate serviceTemplate = serviceTemplates.get(0);
		Assert.assertNotNull(serviceTemplate);
		Assert.assertEquals("strimzi-cluster-operator.v0.32.0", serviceTemplate.getName());
		assertNotNull(serviceTemplate.getCRDs());
		assertEquals(1, serviceTemplate.getCRDs().size());
	}

	@Test
	public void verifyThatServiceTemplatesReturnsCRDInfo() throws IOException {
		List<ServiceTemplate> serviceTemplates = DESERIALIZER
				.fromList(Serialization.unmarshal(srcUrl.openStream(), ClusterServiceVersionList.class));
		Assert.assertNotNull(serviceTemplates);
		Assert.assertEquals(1, serviceTemplates.size());
		ServiceTemplate serviceTemplate = serviceTemplates.get(0);
		Assert.assertNotNull(serviceTemplate);
		Assert.assertEquals("strimzi-cluster-operator.v0.32.0", serviceTemplate.getName());
		assertNotNull(serviceTemplate.getCRDs());
		assertEquals(1, serviceTemplate.getCRDs().size());
		OperatorCRD crd = serviceTemplate.getCRDs().get(0);
		assertEquals("kafkas.kafka.strimzi.io", crd.getName());
		assertEquals("v1beta2", crd.getVersion());
		assertEquals("Kafka", crd.getKind());
		assertEquals("Kafka", crd.getDisplayName());
		assertEquals("Represents a Kafka cluster", crd.getDescription());
		assertNotNull(crd.getSample());
		assertNull(crd.getSchema());
		assertNotNull(crd.getSpecDescriptors());
		assertEquals(7, crd.getSpecDescriptors().size());
		OperatorCRDSpecDescriptor descriptor = crd.getSpecDescriptors().get(0);
		assertEquals("kafka.version", descriptor.getPath());
		assertEquals("Version", descriptor.getDisplayName());
		assertEquals("Kafka version", descriptor.getDescription());
		assertEquals(1, descriptor.getDescriptors().size());
		assertEquals("urn:alm:descriptor:com.tectonic.ui:text", descriptor.getDescriptors().get(0));
	}
}
