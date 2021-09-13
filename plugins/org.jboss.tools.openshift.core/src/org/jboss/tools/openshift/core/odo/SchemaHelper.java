/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.StringTokenizer;

public class SchemaHelper {
    private static final String POD_COUNT_DESCRIPTOR = "urn:alm:descriptor:com.tectonic.ui:podCount";
    private static final String BOOLEAN_SWITCH_DESCRIPTOR = "urn:alm:descriptor:com.tectonic.ui:booleanSwitch";
    private static final String CHECKBOX_DESCRIPTOR = "urn:alm:descriptor:com.tectonic.ui:checkbox";
    private static final String NUMBER_DESCRIPTOR = "urn:alm:descriptor:com.tectonic.ui:number";
    private static final String SELECT_DESCRIPTOR = "urn:alm:descriptor:com.tectonic.ui:select:";

    public static ObjectNode getAnnotatedSchema(ObjectNode schema, List<OperatorCRDSpecDescriptor> descriptors) {
        if (!schema.has("properties")) {
            schema.set("properties", schema.objectNode());
        }
        if (!schema.get("properties").has("spec")) {
            ObjectNode spec = schema.objectNode();
            spec.set("type", spec.textNode("object"));
            ((ObjectNode)schema.get("properties")).set("spec", spec);
        }
        ObjectNode spec = (ObjectNode) schema.get("properties").get("spec");
        for(OperatorCRDSpecDescriptor descriptor : descriptors) {
            ObjectNode node = getSchema(spec, descriptor.getPath());
            if (StringUtils.isNotBlank(descriptor.getDisplayName())) {
                node.set("displayName", node.textNode(descriptor.getDisplayName()));
            }
            if (StringUtils.isNotBlank(descriptor.getDisplayName())) {
                node.set("description", node.textNode(descriptor.getDescription()));
            }
            annotateType(node, descriptor);
        }
        return schema;
    }

    private static void annotateType(ObjectNode node, OperatorCRDSpecDescriptor crdDescriptor) {
        for(String descriptor : crdDescriptor.getDescriptors()) {
            if (POD_COUNT_DESCRIPTOR.equals(descriptor)) {
                node.set("type", node.textNode("integer"));
            } else if (BOOLEAN_SWITCH_DESCRIPTOR.equals(descriptor) || CHECKBOX_DESCRIPTOR.equals(descriptor)) {
                node.set("type", node.textNode("boolean"));
            } else if (NUMBER_DESCRIPTOR.equals(descriptor)) {
                node.set("type", node.textNode("number"));
            } else if (descriptor.startsWith(SELECT_DESCRIPTOR)) {
                if (!node.has("enum")) {
                    node.set("enum", node.arrayNode());
                }
                String select = descriptor.substring(SELECT_DESCRIPTOR.length());
                node.withArray("enum").add(select);
            }
        }
    }

    private static ObjectNode getSchema(ObjectNode schema, String path) {
        ObjectNode parent = schema;
        StringTokenizer st = new StringTokenizer(path, ".");
        while (st.hasMoreTokens()) {
            String p = st.nextToken();
            if (!parent.has("properties")) {
                parent.set("properties", parent.objectNode());
            }
            if (parent.get("properties").has(p)) {
                parent = (ObjectNode) parent.get("properties").get(p);
            } else {
                ObjectNode node = parent.objectNode();
                ((ObjectNode)parent.get("properties")).set(p, node);
                if (st.hasMoreTokens()) {
                    node.set("type", node.textNode("object"));
                } else {
                    node.set("type", node.textNode("string"));
                }
                parent = node;
            }
        }
        return parent;
    }
}
