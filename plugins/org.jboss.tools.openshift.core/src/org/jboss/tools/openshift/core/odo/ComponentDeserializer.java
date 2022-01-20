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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComponentDeserializer extends StdNodeBasedDeserializer<List<Component>> {

    public static final String DEVFILE_FIELD = "devfileComponents";
    public static final String OTHER_FIELD = "otherComponents";
    public static final String METADATA_FIELD = "metadata";
    public static final String STATUS_FIELD = "status";
    public static final String STATE_FIELD = "state";
    public static final String NAME_FIELD = "name";

    public ComponentDeserializer() {
        super(TypeFactory.defaultInstance().constructCollectionType(List.class, Component.class));
    }

    @Override
    public List<Component> convert(JsonNode root, DeserializationContext context) {
        List<Component> result = new ArrayList<>();
        // two roots, devfileComponents and otherComponents
        result.addAll(parseComponents(root.get(DEVFILE_FIELD), ComponentKind.DEVFILE));
        result.addAll(parseComponents(root.get(OTHER_FIELD), ComponentKind.DEVFILE));
        return result;
    }

    private Collection<Component> parseComponents(JsonNode tree, ComponentKind kind) {
        List<Component> result = new ArrayList<>();
        if (tree != null) {
            for (JsonNode item : tree) {
                result.add(Component.of(getName(item), getComponentInfo(item, kind)));
            }
        }
        return result;
    }

    private ComponentInfo getComponentInfo(JsonNode item, ComponentKind kind) {
        JSonParser parser = new JSonParser(item);
        return parser.parseComponentInfo(kind);
    }

    private String getName(JsonNode item) {
        if (item.has(METADATA_FIELD) && item.get(METADATA_FIELD).has(NAME_FIELD)) {
            return item.get(METADATA_FIELD).get(NAME_FIELD).asText();
        } else {
            return "";
        }
    }
}
