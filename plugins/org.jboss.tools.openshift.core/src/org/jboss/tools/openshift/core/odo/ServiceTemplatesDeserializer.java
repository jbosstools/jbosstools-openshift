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

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceTemplatesDeserializer extends StdNodeBasedDeserializer<List<ServiceTemplate>> {
    /**
   * 
   */
  private static final String NAME_FIELD = "name";
    /**
   * 
   */
  private static final String PLAN_LIST_FIELD = "planList";
    /**
   * 
   */
  private static final String SPEC_FIELD = "spec";
    /**
   * 
   */
  private static final String METADATA_FIELD = "metadata";
    /**
   * 
   */
  private static final String ITEMS_FIELD = "items";
    /**
   * 
   */
  private static final String SERVICES_FIELD = "services";

  public ServiceTemplatesDeserializer() {
    super(TypeFactory.defaultInstance().constructCollectionType(List.class, ServiceTemplate.class));
  }

  @Override
  public List<ServiceTemplate> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
    List<ServiceTemplate> result = new ArrayList<>();
    JsonNode services = root.get(SERVICES_FIELD);
    if (services != null) {
      JsonNode items = services.get(ITEMS_FIELD);
      if (items != null) {
        for (JsonNode item : items) {
          String name = item.get(METADATA_FIELD).get(NAME_FIELD).asText();
          List<String> plans = new ArrayList<>();
          for (JsonNode plan : item.get(SPEC_FIELD).get(PLAN_LIST_FIELD)) {
            plans.add(plan.asText());
          }
          result.add(new ServiceTemplate() {

            @Override
            public String getName() {
              return name;
            }

            @Override
            public List<String> getPlans() {
              return plans;
            }
          });
        }
      }
    }
    return result;
  }
}