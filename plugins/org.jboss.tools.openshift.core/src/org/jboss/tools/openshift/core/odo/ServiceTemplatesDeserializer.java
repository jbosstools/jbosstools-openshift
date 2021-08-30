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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ServiceTemplatesDeserializer extends StdNodeBasedDeserializer<List<ServiceTemplate>> {
  private static final String ITEMS_FIELD = "items";
  private static final String METADATA_FIELD = "metadata";
  private static final String NAME_FIELD = "name";
  private static final String SPEC_FIELD = "spec";
  private static final String OPERATORS_FIELD = "operators";
  private static final String ANNOTATIONS_FIELD = "annotations";
  private static final String ALM_EXAMPLES_FIELD = "alm-examples";
  private static final String CRD_FIELD = "customresourcedefinitions";
  private static final String OWNED_FIELD = "owned";
  private static final String VERSION_FIELD = "version";
  private static final String KIND_FIELD = "kind";
  private static final String DISPLAY_NAME_FIELD = "displayName";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String API_VERSION_FIELD = "apiVersion";
  
  private static final ObjectMapper MAPPER = new ObjectMapper();
  
  private final Function<String, JsonNode> schemaMapper;

  public ServiceTemplatesDeserializer(Function<String, JsonNode> schemaMapper) {
    super(TypeFactory.defaultInstance().constructCollectionType(List.class, ServiceTemplate.class));
    this.schemaMapper = schemaMapper;
  }

  @Override
  public List<ServiceTemplate> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
    List<ServiceTemplate> result = new ArrayList<>();
    convertOperators(root, result);
    return result;
  }
  
  private void convertOperators(JsonNode root, List<ServiceTemplate> result) throws JsonProcessingException {
    JsonNode operators = root.get(OPERATORS_FIELD);
    if (operators != null) {
      JsonNode items = operators.get(ITEMS_FIELD);
      if (items != null) {
        for (JsonNode item : items) {
          String name = item.get(METADATA_FIELD).get(NAME_FIELD).asText();
          String displayName = item.get(SPEC_FIELD).has(DISPLAY_NAME_FIELD)
              ? item.get(SPEC_FIELD).get(DISPLAY_NAME_FIELD).asText()
              : name;
          ArrayNode samples = null;
          if (item.get(METADATA_FIELD).has(ANNOTATIONS_FIELD)
              && item.get(METADATA_FIELD).get(ANNOTATIONS_FIELD).has(ALM_EXAMPLES_FIELD)) {
            samples = (ArrayNode) MAPPER
                .readTree(item.get(METADATA_FIELD).get(ANNOTATIONS_FIELD).get(ALM_EXAMPLES_FIELD).asText());

          }
          List<OperatorCRD> crds = new ArrayList<>();
          for (JsonNode crd : item.get(SPEC_FIELD).get(CRD_FIELD).get(OWNED_FIELD)) {
            ArrayNode finalSamples = samples;
            crds.add(new OperatorCRD() {
              @Override
              public String getName() {
                return crd.get(NAME_FIELD).asText();
              }

              @Override
              public String getVersion() {
                return crd.get(VERSION_FIELD).asText();
              }

              @Override
              public String getKind() {
                return crd.get(KIND_FIELD).asText();
              }

              @Override
              public String getDisplayName() {
                return crd.get(DISPLAY_NAME_FIELD).asText();
              }

              @Override
              public String getDescription() {
                return crd.get(DESCRIPTION_FIELD).asText();
              }

              @Override
              public JsonNode getSample() {
                if (finalSamples != null) {
                  return selectSample(finalSamples, this);
                }
                return null;
              }

              @Override
              public JsonNode getSchema() {
                return schemaMapper.apply(getCRDPrefix(this) + "/namespaces/{namespace}/" + getCRDSuffix(this));
              }

              @Override
              public List<OperatorCRDSpecDescriptor> getSpecDescriptors() {
                return Collections.emptyList();
              }
            });
          }
          result.add(new ServiceTemplate() {
            @Override
            public String getName() {
              return name;
            }

            @Override
            public String getDisplayName() {
              return displayName;
            }

            @Override
            public List<OperatorCRD> getCRDs() {
              return crds;
            }
          });
        }
      }
    }
  }

  private JsonNode selectSample(ArrayNode samples, OperatorCRD crd) {
    String name = getCRDPrefix(crd);
    JsonNode result = null;
    for (JsonNode node : samples) {
      if (node.has(API_VERSION_FIELD) && node.get(API_VERSION_FIELD).asText().equals(name) && node.has(KIND_FIELD)
          && node.get(KIND_FIELD).asText().equals(crd.getKind())) {
        result = node;
        break;
      }
    }
    return result;
  }

  private String getCRDPrefix(OperatorCRD crd) {
    String name = crd.getName();
    name = name.substring(name.indexOf('.') + 1) + '/' + crd.getVersion();
    return name;
  }

  private String getCRDSuffix(OperatorCRD crd) {
    String name = crd.getName();
    if (name.indexOf('.') != (-1)) {
      name = name.substring(0, name.indexOf('.'));
    }
    return name;
  }
}