/*******************************************************************************
 * Copyright (c) 2019-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import static org.jboss.tools.openshift.core.odo.JSonParser.get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ComponentTypesDeserializer extends StdNodeBasedDeserializer<List<ComponentType>> {
	private static final long serialVersionUID = 7595264268359691655L;

	private static final String DEVFILE_NAME_FIELD = "name";

	private static final String DEVFILE_REGISTRY = "registry";

	private static final String DEVFILE_DISPLAY_NAME_FIELD = "displayName";

	private static final String DEVFILE_DESCRIPTION_FIELD = "description";

	private static final String DEVFILE_LANGUAGE_FIELD = "language";

	private static final String DEVFILE_TAGS_FIELD = "tags";

	public ComponentTypesDeserializer() {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, ComponentType.class));
	}

	@Override
	public List<ComponentType> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		List<ComponentType> result = new ArrayList<>();
		result.addAll(parseDevfileItems(root));
		return result;
	}

	private List<ComponentType> parseDevfileItems(JsonNode items) {
		List<ComponentType> result = new ArrayList<>();
		if (items != null) {
			for (JsonNode item : items) {
				String name = get(item, DEVFILE_NAME_FIELD);
				String displayName = get(item, DEVFILE_DISPLAY_NAME_FIELD);
				String description = get(item, DEVFILE_DESCRIPTION_FIELD);
				String language = get(item, DEVFILE_LANGUAGE_FIELD);
				List<String> tags = new ArrayList<>();
				if (item.has(DEVFILE_TAGS_FIELD)) {
					item.get(DEVFILE_TAGS_FIELD).elements().forEachRemaining(node -> tags.add(node.asText()));
				}
				DevfileRegistry registry = DevfileRegistriesDeserializer.getRegistry(item.get(DEVFILE_REGISTRY));
				result.add(new DevfileComponentType(name, displayName, description, registry, language, tags));
			}
		}
		return result;
	}
}
