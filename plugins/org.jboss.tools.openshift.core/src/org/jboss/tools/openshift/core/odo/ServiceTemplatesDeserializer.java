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
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * @author Red Hat Developers
 *
 */
public class ServiceTemplatesDeserializer extends StdNodeBasedDeserializer<List<ServiceTemplate>> {
    public ServiceTemplatesDeserializer() {
        super(TypeFactory.defaultInstance().constructCollectionType(List.class, ServiceTemplate.class));
    }

    @Override
    public List<ServiceTemplate> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
        List<ServiceTemplate> result = new ArrayList<>();
        JsonNode items = root.get("items");
        if (items != null) {
            for (JsonNode item : items) {
                String name = item.get("metadata").get("name").asText();
                //TODO manage plan lists.
                String plan = item.get("spec").get("planList").get(0).asText();
                result.add(new ServiceTemplate() {

                    @Override
                    public String getName() {
                        return name;
                    }

                    @Override
                    public String getPlan() {
                        return plan;
                    }
                });
            }
        }
        return result;
    }
}
