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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class JSonParser {
    private static final String ITEMS_FIELD = "items";
    private static final String METADATA_FIELD = "metadata";
    private static final String NAME_FIELD = "name";
    private static final String SPEC_FIELD = "spec";
    private static final String SOURCE_TYPE_FIELD = "sourceType";
    private static final String TYPE_FIELD = "type";
    private static final String ENV_FIELD = "env";
    private static final String VALUE_FIELD = "value";

    private final JsonNode root;

    public JSonParser(JsonNode root) {
        this.root = root;
    }

    public List<URL> parseURLS() {
        List<URL> result = new ArrayList<>();
        if (root.has(ITEMS_FIELD)) {
            root.get(ITEMS_FIELD).forEach(item -> {
                //odo incorrectly reports urls created with the web ui without names
                if (item.get(METADATA_FIELD).has(NAME_FIELD)) {
                    String name = item.get(METADATA_FIELD).get(NAME_FIELD).asText();
                    String protocol = item.get(SPEC_FIELD).has("protocol") ?
                            item.get(SPEC_FIELD).get("protocol").asText() : "";
                    String host = item.get(SPEC_FIELD).has("host") ?
                            item.get(SPEC_FIELD).get("host").asText() : "";
                    String port = item.get(SPEC_FIELD).has("port") ? item.get(SPEC_FIELD).get("port").asText() : "0";
                    result.add(URL.of(name, protocol, host, port, item.get("status").get("state").asText(), item.get(SPEC_FIELD).get("secure").asBoolean()));
                }
            });
        }
        return result;
    }

    public List<Application> parseApplications() {
        List<Application> result = new ArrayList<>();
        if (root.has(ITEMS_FIELD)) {
            root.get(ITEMS_FIELD).forEach(item -> result.add(Application.of(item.get(METADATA_FIELD).get(NAME_FIELD).asText())));
        }
        return result;
    }

    public ComponentInfo parseComponentInfo(ComponentKind kind) {
        ComponentInfo.Builder builder = new ComponentInfo.Builder().withComponentKind(kind);
        if (root.has(SPEC_FIELD)) {
            String componentTypeName = root.get(SPEC_FIELD).get(TYPE_FIELD).asText();
            if (root.get(SPEC_FIELD).has(SOURCE_TYPE_FIELD)) {
                String sourceType = root.get(SPEC_FIELD).get(SOURCE_TYPE_FIELD).asText();
                builder.withSourceType(ComponentSourceType.fromAnnotation(sourceType)).withComponentTypeName(componentTypeName);
            } else {
                builder.withSourceType(ComponentSourceType.LOCAL).withComponentTypeName(componentTypeName);
            }
            if (root.get(SPEC_FIELD).has(ENV_FIELD)) {
                JsonNode env = root.get(SPEC_FIELD).get(ENV_FIELD);
                for(JsonNode elt : env) {
                    if (elt.has(NAME_FIELD) && elt.has(VALUE_FIELD)) {
                        builder.addEnv(elt.get(NAME_FIELD).asText(), elt.get(VALUE_FIELD).asText());
                    }
                }
            }
        }
        return builder.build();
    }
}
