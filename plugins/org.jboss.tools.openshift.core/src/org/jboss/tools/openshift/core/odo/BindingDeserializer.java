/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class BindingDeserializer extends StdNodeBasedDeserializer<List<Binding>> {
	private static final long serialVersionUID = -4994881756748477189L;

	private static final String NAME_FIELD = "name";

	private static final String SPEC_FIELD = "spec";

	private static final String SERVICES_FIELD = "services";

	private static final String KIND_FIELD = "kind";
	private static final String APIVERSION_FIELD = "apiVersion";
	public static final String BINDABLE_SERVICES_FIELD = "bindableServices";
	private static final String STATUS_FIELD = "status";
	private static final String BINDING_ENV_VARS_FIELD = "bindingEnvVars";

	public BindingDeserializer() {
		super(TypeFactory.defaultInstance().constructCollectionType(List.class, Binding.class));
	}

	@Override
	public List<Binding> convert(JsonNode root, DeserializationContext ctxt) throws IOException {
		List<Binding> result = new ArrayList<>();
		for (JsonNode binding : root) {
			result.add(getBinding(binding));
		}
		return result;
	}

	public static Binding getBinding(JsonNode binding) {
		var name = binding.get(NAME_FIELD).asText();
		if (binding.has(SPEC_FIELD) && binding.get(SPEC_FIELD).has(SERVICES_FIELD)) {
			var serviceNode = binding.get(SPEC_FIELD).get(SERVICES_FIELD).get(0);
			if (serviceNode.has(KIND_FIELD) && serviceNode.has(NAME_FIELD) && serviceNode.has(APIVERSION_FIELD)) {
				var service = Service.of(serviceNode.get(NAME_FIELD).asText(),
						serviceNode.get(APIVERSION_FIELD).asText(), serviceNode.get(KIND_FIELD).asText());
				List<String> bindingEnvVars = Collections.emptyList();
				if (binding.has(STATUS_FIELD) && binding.get(STATUS_FIELD).has(BINDING_ENV_VARS_FIELD)) {
					bindingEnvVars = new ArrayList<>();
					for (var bindingEnvVar : binding.get(STATUS_FIELD).get(BINDING_ENV_VARS_FIELD)) {
						bindingEnvVars.add(bindingEnvVar.asText());
					}
				}
				return Binding.of(name, service, bindingEnvVars);
			}
		}
		return null;
	}
}