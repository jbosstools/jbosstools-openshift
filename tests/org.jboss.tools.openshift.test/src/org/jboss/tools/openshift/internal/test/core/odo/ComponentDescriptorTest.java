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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.jboss.tools.openshift.core.odo.ComponentDescriptor;
import org.jboss.tools.openshift.core.odo.ComponentDescriptorsDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ComponentDescriptorTest {
  private static final URL url = ComponentDescriptorTest.class.getResource("/component-descriptor-test.json");

  private static ObjectMapper MAPPER;

  @BeforeClass
  public static void setup() {
    MAPPER = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, new ComponentDescriptorsDeserializer());
    MAPPER.registerModule(module);
  }

  @Test
  public void verifyThatComponentDescriptorsCanLoad() throws IOException {
    List<ComponentDescriptor> descriptors = MAPPER.readValue(url, new TypeReference<List<ComponentDescriptor>>() {});
    Assert.assertNotNull(descriptors);
  }

  @Test
  public void verifyThatComponentDescriptorsReturnsComponentDescriptor() throws IOException {
    List<ComponentDescriptor> descriptors = MAPPER.readValue(url, new TypeReference<List<ComponentDescriptor>>() {});
    Assert.assertNotNull(descriptors);
    Assert.assertEquals(1, descriptors.size());
    Assert.assertNotNull(descriptors.get(0));
  }

  @Test
  public void verifyThatComponentDescriptorsReturnsComponentDescriptorProperties() throws IOException {
    List<ComponentDescriptor> descriptors = MAPPER.readValue(url, new TypeReference<List<ComponentDescriptor>>() {});
    Assert.assertNotNull(descriptors);
    Assert.assertEquals(1, descriptors.size());
    ComponentDescriptor descriptor = descriptors.get(0);
    Assert.assertNotNull(descriptor);
    Assert.assertEquals("sbapp", descriptor.getApplication());
    Assert.assertEquals("testodo", descriptor.getProject());
    Assert.assertEquals("sbcomp", descriptor.getName());
    Assert.assertNotNull(descriptor.getPorts());
    List<Integer> ports = descriptor.getPorts();
    Assert.assertEquals(3, ports.size());
    Assert.assertEquals(8080, ports.get(0).intValue());
    Assert.assertEquals(8443, ports.get(1).intValue());
    Assert.assertEquals(8778, ports.get(2).intValue());
  }
}
