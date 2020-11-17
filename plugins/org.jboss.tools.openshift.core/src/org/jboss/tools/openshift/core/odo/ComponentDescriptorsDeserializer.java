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
package org.jboss.tools.openshift.core.odo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author Red Hat Developers
 *
 */
public class ComponentDescriptorsDeserializer extends StdNodeBasedDeserializer<List<ComponentDescriptor>> {
  private static final String APP_FIELD = "app";

  private static final String NAME_FIELD = "name";

  private static final String CONTEXT_FIELD = "context";

  private static final String S2I_COMPONENTS_FIELD = "s2iComponents";
  
  private static final String DEVFILE_COMPONENTS_FIELD = "devfileComponents";

  private static final String NAMESPACE_FIELD = "namespace";

  private static final String PORTS_FIELD = "ports";

  private static final String SPEC_FIELD = "spec";

  private static final String STATUS_FIELD = "status";

  private static final String METADATA_FIELD = "metadata";

  public ComponentDescriptorsDeserializer() {
    super(TypeFactory.defaultInstance().constructCollectionType(List.class, ComponentDescriptor.class));
  }

  @Override
  public List<ComponentDescriptor> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
    List<ComponentDescriptor> result = new ArrayList<>();
    parseComponents(result, root.get(DEVFILE_COMPONENTS_FIELD), ComponentKind.DEVFILE);
    parseComponents(result, root.get(S2I_COMPONENTS_FIELD), ComponentKind.S2I);
    return result;
  }

  private void parseComponents(List<ComponentDescriptor> result, JsonNode items, ComponentKind kind) {
    if (items != null) {
      for (Iterator<JsonNode> it = items.iterator(); it.hasNext();) {
        JsonNode item = it.next();
        result.add(new ComponentDescriptor(getProject(item), getApplication(item), getPath(item), getName(item),
            kind, getPorts(item)));
      }
    }
  }

  private List<Integer> getPorts(JsonNode item) {
    List<Integer> ports = new ArrayList<>();
    if (item.has(SPEC_FIELD) && item.get(SPEC_FIELD).has(PORTS_FIELD)) {
      for (JsonNode portNode : item.get(SPEC_FIELD).get(PORTS_FIELD)) {
        String port = portNode.asText();
        if (port.endsWith("/TCP")) {
          ports.add(Integer.parseInt(port.substring(0, port.length() - 4)));
        } else {
          ports.add(Integer.parseInt(port));
        }
      }
    }
    return ports;
  }

  private String getProject(JsonNode item) {
    if (item.has(METADATA_FIELD) && item.get(METADATA_FIELD).has(NAMESPACE_FIELD)) {
      return item.get(METADATA_FIELD).get(NAMESPACE_FIELD).asText();
    } else {
      return "";
    }
  }

  private String getApplication(JsonNode item) {
    if (item.has(SPEC_FIELD) && item.get(SPEC_FIELD).has(APP_FIELD)) {
      return item.get(SPEC_FIELD).get(APP_FIELD).asText();
    } else {
      return "";
    }
  }

  private String getPath(JsonNode item) {
    if (item.has(STATUS_FIELD) && item.get(STATUS_FIELD).has(CONTEXT_FIELD)) {
      return item.get(STATUS_FIELD).get(CONTEXT_FIELD).asText();
    } else {
      return "";
    }
  }

  private String getName(JsonNode item) {
    if (item.has(METADATA_FIELD) && item.get(METADATA_FIELD).has(NAME_FIELD)) {
      return item.get(METADATA_FIELD).get(NAME_FIELD).asText();
    } else {
      return "";
    }
  }
}
