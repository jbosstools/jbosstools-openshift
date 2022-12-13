/*******************************************************************************
 * Copyright (c) 2021-2022 Red Hat, Inc.
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

public class DevfileRegistriesDeserializer extends StdNodeBasedDeserializer<List<DevfileRegistry>> {
	private static final long serialVersionUID = -2290623576338522754L;

	private static final String NAME_FIELD = "name"; //$NON-NLS-1$

	private static final String URL_FIELD = "url"; //$NON-NLS-1$

	private static final String SECURE_FIELD = "secure"; //$NON-NLS-1$

	private static final String REGISTRIES_FIELD = "registries"; //$NON-NLS-1$

	public DevfileRegistriesDeserializer() {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, DevfileRegistry.class));
	}

	@Override
	public List<DevfileRegistry> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		List<DevfileRegistry> result = new ArrayList<>();
		JsonNode registries = root.get(REGISTRIES_FIELD);
		if (registries != null) {
			for (JsonNode registry : registries) {
				result.add(getRegistry(registry));
			}
		}
		return result;
	}

	public static DevfileRegistry getRegistry(JsonNode registry) {
		return DevfileRegistry.of(registry.get(NAME_FIELD).asText(), registry.get(URL_FIELD).asText(),
				registry.get(SECURE_FIELD).asBoolean());
	}
}