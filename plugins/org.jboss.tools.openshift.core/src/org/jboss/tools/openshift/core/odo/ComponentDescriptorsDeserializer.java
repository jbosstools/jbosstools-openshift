/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import static org.jboss.tools.openshift.core.odo.ComponentDeserializer.COMPONENTS_FIELD;
import static org.jboss.tools.openshift.core.odo.ComponentDeserializer.NAME_FIELD;
import static org.jboss.tools.openshift.core.odo.JSonParser.get;

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
public class ComponentDescriptorsDeserializer extends StdNodeBasedDeserializer<List<ComponentDescriptor>> {
	private static final long serialVersionUID = 3344841964691264621L;
	public static final String COMPONENT_IN_DEVFILE_FIELD = "componentInDevfile";
	private static final String MANAGED_BY_FIELD = "managedBy";
	private static final String MANAGED_BY_VERSION_FIELD = "managedByVersion";
	private final String path;

	public ComponentDescriptorsDeserializer(String path) {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, ComponentDescriptor.class));
		this.path = path;
	}

	@Override
	public List<ComponentDescriptor> convert(JsonNode root, DeserializationContext context) {
		List<ComponentDescriptor> result = new ArrayList<>();
		if (root.has(COMPONENT_IN_DEVFILE_FIELD)) {
			String devFileName = root.get(COMPONENT_IN_DEVFILE_FIELD).asText();
			if (root.has(COMPONENTS_FIELD)) {
				for (JsonNode node : root.get(COMPONENTS_FIELD)) {
					String name = get(node, NAME_FIELD);
					String managedBy = get(node, MANAGED_BY_FIELD);
					String managedByVersion = get(node, MANAGED_BY_VERSION_FIELD);
					if (devFileName.equals(name)) {
						result.add(new ComponentDescriptor(devFileName, path, managedBy, managedByVersion));
					}
				}
			}
		}
		return result;
	}
}
