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

import org.jboss.tools.openshift.core.odo.Storage;
import org.jboss.tools.openshift.core.odo.StoragesDeserializer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class StorageTest {
  private static final URL url = StorageTest.class.getResource("/storage-test.json");

  private static ObjectMapper MAPPER;

  @BeforeClass
  public static void setup() {
    MAPPER = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, new StoragesDeserializer());
    MAPPER.registerModule(module);
  }

  @Test
  public void verifyThatStoragesCanLoad() throws IOException {
    List<Storage> storages = MAPPER.readValue(url, new TypeReference<List<Storage>>() {});
    Assert.assertNotNull(storages);
  }

  @Test
  public void verifyThatStoragesReturnsComponentDescriptor() throws IOException {
    List<Storage> storages = MAPPER.readValue(url, new TypeReference<List<Storage>>() {});
    Assert.assertNotNull(storages);
    Assert.assertEquals(1, storages.size());
    Assert.assertNotNull(storages.get(0));
  }

  @Test
  public void verifyThatStoragesReturnsComponentDescriptorProperties() throws IOException {
    List<Storage> storages = MAPPER.readValue(url, new TypeReference<List<Storage>>() {});
    Assert.assertNotNull(storages);
    Assert.assertEquals(1, storages.size());
    Storage storage = storages.get(0);
    Assert.assertNotNull(storage);
    Assert.assertEquals("stor1", storage.getName());
  }
}
