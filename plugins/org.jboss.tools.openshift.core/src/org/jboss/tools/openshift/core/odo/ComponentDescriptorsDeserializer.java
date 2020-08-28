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
	public ComponentDescriptorsDeserializer() {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, ComponentDescriptor.class));
	}

	@Override
	public List<ComponentDescriptor> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		List<ComponentDescriptor> result = new ArrayList();
		JsonNode items = root.get("items");
		if (items != null) {
			for (Iterator<JsonNode> it = items.iterator(); it.hasNext();) {
				JsonNode item = it.next();
				result.add(new ComponentDescriptor(getProject(item), getApplication(item), getPath(item), getName(item),
				    getPorts(item)));
			}
		}
		return result;
	}

	private List<Integer> getPorts(JsonNode item) {
		List<Integer> ports = new ArrayList<>();
		if (item.has("spec") && item.get("spec").has("ports")) {
			for (JsonNode portNode : item.get("spec").get("ports")) {
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
		if (item.has("metadata") && item.get("metadata").has("namespace")) {
			return item.get("metadata").get("namespace").asText();
		} else {
			return "";
		}
	}

	private String getApplication(JsonNode item) {
		if (item.has("spec") && item.get("spec").has("app")) {
			return item.get("spec").get("app").asText();
		} else {
			return "";
		}
	}

	private String getPath(JsonNode item) {
		if (item.has("status") && item.get("status").has("context")) {
			return item.get("status").get("context").asText();
		} else {
			return "";
		}
	}

	private String getName(JsonNode item) {
		if (item.has("metadata") && item.get("metadata").has("name")) {
			return item.get("metadata").get("name").asText();
		} else {
			return "";
		}
	}
}
