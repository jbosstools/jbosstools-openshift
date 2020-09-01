/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ComponentTypesDeserializer extends StdNodeBasedDeserializer<List<ComponentType>> {
    /**
   * 
   */
  private static final String NON_HIDDEN_TAGS_FIELD = "nonHiddenTags";
    /**
   * 
   */
  private static final String SPEC_FIELD = "spec";
    /**
   * 
   */
  private static final String NAME_FIELD = "name";
    /**
   * 
   */
  private static final String METADATA__FIELD = "metadata";
    /**
   * 
   */
  private static final String ITEMS_FIELD = "items";
  
  public ComponentTypesDeserializer() {
    super(TypeFactory.defaultInstance().constructCollectionType(List.class, ComponentType.class));
  }

  @Override
  public List<ComponentType> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
    List<ComponentType> result = new ArrayList<>();
    JsonNode items = root.get(ITEMS_FIELD);
    if (items != null) {
      for (Iterator<JsonNode> it = items.iterator(); it.hasNext();) {
        JsonNode item = it.next();
        String name = item.get(METADATA__FIELD).get(NAME_FIELD).asText();
        List<String> versions = new ArrayList<>();
        item.get(SPEC_FIELD).get(NON_HIDDEN_TAGS_FIELD).forEach(node -> versions.add(node.textValue()));
        result.add(new ComponentType() {

          @Override
          public String getName() {
            return name;
          }

          @Override
          public String[] getVersions() {
            return versions.toArray(new String[versions.size()]);
          }
        });
      }
    }
    return result;
  }
}
