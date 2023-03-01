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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ServiceDeserializer extends StdNodeBasedDeserializer<List<Service>> {
	private static final long serialVersionUID = 3410206923429616866L;

	private static final String NAME_FIELD = "name";

	private static final String KIND_FIELD = "kind";
	private static final String APIVERSION_FIELD = "apiVersion";
	public static final String BINDABLE_SERVICES_FIELD = "bindableServices";

	public ServiceDeserializer() {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, Service.class));
	}

	@Override
	public List<Service> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		List<Service> result = new ArrayList<>();
		if (root.has(BINDABLE_SERVICES_FIELD)) {
			for (JsonNode service : root.get(BINDABLE_SERVICES_FIELD)) {
				result.add(getService(service));
			}
		}
		return result;
	}

	public static Service getService(JsonNode service) {
		String apiVersion = service.get(APIVERSION_FIELD).asText();
		String kind = service.get(KIND_FIELD).asText();
		return Service.of(service.get(NAME_FIELD).asText(), apiVersion, kind);
	}
}