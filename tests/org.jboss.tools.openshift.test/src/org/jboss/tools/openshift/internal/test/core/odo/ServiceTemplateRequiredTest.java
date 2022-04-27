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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.core.odo.ServiceTemplatesDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ServiceTemplateRequiredTest {
  private static final URL url = ServiceTemplateRequiredTest.class.getResource("/service-template-required-test.json");

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
    Assert.assertEquals(6, serviceTemplates.size());
  }
}
