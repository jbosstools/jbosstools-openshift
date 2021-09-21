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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.OperatorCRDSpecDescriptor;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServiceTemplateTest {
  private static final URL url = ServiceTemplateTest.class.getResource("/service-template-test.json");

  private static ObjectMapper MAPPER;

  @BeforeClass
  public static void setup() {
    MAPPER = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, new ServiceTemplatesDeserializer(s -> null));
    MAPPER.registerModule(module);
  }

  @Test
  public void verifyThatServiceTemplatesCanLoad() throws IOException {
    List<ServiceTemplate> serviceTemplates = MAPPER.readValue(url, new TypeReference<List<ServiceTemplate>>() {});
    Assert.assertNotNull(serviceTemplates);
  }

  @Test
  public void verifyThatServiceTemplatesReturnsItems() throws IOException {
    List<ServiceTemplate> serviceTemplates = MAPPER.readValue(url, new TypeReference<List<ServiceTemplate>>() {});
    Assert.assertNotNull(serviceTemplates);
    Assert.assertEquals(1, serviceTemplates.size());
    Assert.assertNotNull(serviceTemplates.get(0));
  }

  @Test
  public void verifyThatServiceTemplatesReturnsName() throws IOException {
    List<ServiceTemplate> serviceTemplates = MAPPER.readValue(url, new TypeReference<List<ServiceTemplate>>() {});
    Assert.assertNotNull(serviceTemplates);
    Assert.assertEquals(1, serviceTemplates.size());
    ServiceTemplate serviceTemplate = serviceTemplates.get(0);
    Assert.assertNotNull(serviceTemplate);
    Assert.assertEquals("strimzi-cluster-operator.v0.25.0", serviceTemplate.getName());
  }

  @Test
  public void verifyThatServiceTemplatesReturnsCRDs() throws IOException {
    List<ServiceTemplate> serviceTemplates = MAPPER.readValue(url, new TypeReference<List<ServiceTemplate>>() {});
    Assert.assertNotNull(serviceTemplates);
    Assert.assertEquals(1, serviceTemplates.size());
    ServiceTemplate serviceTemplate = serviceTemplates.get(0);
    Assert.assertNotNull(serviceTemplate);
    Assert.assertEquals("strimzi-cluster-operator.v0.25.0", serviceTemplate.getName());
    assertTrue(serviceTemplate instanceof ServiceTemplate);
    ServiceTemplate operatorServiceTemplate = (ServiceTemplate) serviceTemplate;
    assertNotNull(operatorServiceTemplate.getCRDs());
    assertEquals(9, operatorServiceTemplate.getCRDs().size());
  }

  @Test
  public void verifyThatServiceTemplatesReturnsCRDInfo() throws IOException {
    List<ServiceTemplate> serviceTemplates = MAPPER.readValue(url, new TypeReference<List<ServiceTemplate>>() {});
    Assert.assertNotNull(serviceTemplates);
    Assert.assertEquals(1, serviceTemplates.size());
    ServiceTemplate serviceTemplate = serviceTemplates.get(0);
    Assert.assertNotNull(serviceTemplate);
    Assert.assertEquals("strimzi-cluster-operator.v0.25.0", serviceTemplate.getName());
    assertTrue(serviceTemplate instanceof ServiceTemplate);
    ServiceTemplate operatorServiceTemplate = (ServiceTemplate) serviceTemplate;
    assertNotNull(operatorServiceTemplate.getCRDs());
    assertEquals(9, operatorServiceTemplate.getCRDs().size());
    OperatorCRD crd = operatorServiceTemplate.getCRDs().get(0);
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
