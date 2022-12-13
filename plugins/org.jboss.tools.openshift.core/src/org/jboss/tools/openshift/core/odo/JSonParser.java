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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JSonParser {
	private static final String METADATA_FIELD = "metadata";
	private static final String NAME_FIELD = "name";
	private static final String SPEC_FIELD = "spec";
	private static final String PROJECT_TYPE_FIELD = "projectType";
	private static final String DEVFILE_FIELD = "devfile";
	private static final String DEVFILE_DATA_FIELD = "devfileData";
	private static final String STARTER_PROJECTS_FIELD = "starterProjects";
	private static final String DEBUG_PROCESS_ID_FIELD = "debugProcessID";
	private static final String PATHS_FIELD = "paths";
	private static final String POST_FIELD = "post";
	private static final String PARAMETERS_FIELD = "parameters";
	private static final String BODY_VALUE = "body";
	private static final String SCHEMA_FIELD = "schema";
	private static final String DOLLAR_REF_FIELD = "$ref";
	private static final String SUPPORTED_ODO_FEATURES_FIELD = "supportedOdoFeatures";
	private static final String LOCAL_ADDRESS_FIELD = "localAddress";
	private static final String LOCAL_PORT_FIELD = "localPort";
	private static final String CONTAINER_PORT_FIELD = "containerPort";
	private static final String DEV_FORWARDED_PORTS_FIELD = "devForwardedPorts";
	private static final String RUNNING_IN_FIELD = "runningIn";
	private static final String CONTAINER_NAME_FIELD = "containerName";
	private static final String LANGUAGE_FIELD = "language";

	private static final String ENV_FIELD = "env";
	private static final String VALUE_FIELD = "value";

	private final JsonNode root;

	public JSonParser(JsonNode root) {
		this.root = root;
	}

	static String get(JsonNode node, String name) {
		return node.has(name) ? node.get(name).asText() : "";
	}

	public List<URL> parseURLS() {
		List<URL> result = new ArrayList<>();
		if (root.has(DEV_FORWARDED_PORTS_FIELD)) {
			result.addAll(parseURLItems(root.get(DEV_FORWARDED_PORTS_FIELD)));
		}
		return result;
	}

	private List<URL> parseURLItems(JsonNode urlItems) {
		List<URL> result = new ArrayList<>();
		urlItems.forEach(item -> {
			// odo incorrectly reports urls created with the web ui without names
			if (item.has(CONTAINER_NAME_FIELD)) {
				String name = item.get(CONTAINER_NAME_FIELD).asText();
				String host = item.has(LOCAL_ADDRESS_FIELD) ? item.get(LOCAL_ADDRESS_FIELD).asText() : "localhost";
				String localPort = item.has(LOCAL_PORT_FIELD) ? item.get(LOCAL_PORT_FIELD).asText() : "8080";
				String containerPort = item.has(CONTAINER_PORT_FIELD) ? item.get(CONTAINER_PORT_FIELD).asText()
						: "8080";
				result.add(URL.of(name, host, localPort, containerPort));
			}
		});
		return result;
	}

	public ComponentInfo parseComponentInfo(ComponentKind kind) {
		ComponentInfo.Builder builder = new ComponentInfo.Builder().withComponentKind(kind);
		if (root.has(PROJECT_TYPE_FIELD)) {
			String componentTypeName = root.get(PROJECT_TYPE_FIELD).asText();
			builder.withComponentTypeName(componentTypeName);
		}
		if (root.has(SPEC_FIELD) && root.get(SPEC_FIELD).has(ENV_FIELD)) {
			JsonNode env = root.get(SPEC_FIELD).get(ENV_FIELD);
			for (JsonNode elt : env) {
				if (elt.has(NAME_FIELD) && elt.has(VALUE_FIELD)) {
					builder.addEnv(elt.get(NAME_FIELD).asText(), elt.get(VALUE_FIELD).asText());
				}
			}
		}
		return builder.build();
	}

	public ComponentInfo parseDescribeComponentInfo(ComponentKind kind) {
		ComponentInfo.Builder builder = new ComponentInfo.Builder().withComponentKind(kind);
		if (root.has(DEVFILE_DATA_FIELD) && root.get(DEVFILE_DATA_FIELD).has(DEVFILE_FIELD)
				&& root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).has(METADATA_FIELD)
				&& root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).get(METADATA_FIELD).has(PROJECT_TYPE_FIELD)) {
			String componentTypeName = root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).get(METADATA_FIELD)
					.get(PROJECT_TYPE_FIELD).asText();
			builder.withComponentTypeName(componentTypeName);
		}
		if (root.has(DEVFILE_DATA_FIELD) && root.get(DEVFILE_DATA_FIELD).has(DEVFILE_FIELD)
				&& root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).has(METADATA_FIELD)
				&& root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).get(METADATA_FIELD).has(LANGUAGE_FIELD)) {
			String language = root.get(DEVFILE_DATA_FIELD).get(DEVFILE_FIELD).get(METADATA_FIELD).get(LANGUAGE_FIELD)
					.asText();
			builder.withLanguage(language);
		}
		ComponentFeatures features = new ComponentFeatures();
		if (root.has(DEVFILE_DATA_FIELD) && root.get(DEVFILE_DATA_FIELD).has(SUPPORTED_ODO_FEATURES_FIELD)) {
			JsonNode featuresNode = root.get(DEVFILE_DATA_FIELD).get(SUPPORTED_ODO_FEATURES_FIELD);
			getComponentsFeatures(features, featuresNode);
		}
		builder.withFeatures(features);
		return builder.build();
	}

	private static void getComponentsFeatures(ComponentFeatures features, JsonNode featuresNode) {
		if (featuresNode.has(ComponentFeature.DEV.getLabel().toLowerCase())
				&& featuresNode.get(ComponentFeature.DEV.getLabel().toLowerCase()).asBoolean()) {
			features.addFeature(ComponentFeature.DEV);
		}
		if (featuresNode.has(ComponentFeature.DEBUG.getLabel().toLowerCase())
				&& featuresNode.get(ComponentFeature.DEBUG.getLabel().toLowerCase()).asBoolean()) {
			features.addFeature(ComponentFeature.DEBUG);
		}
		if (featuresNode.has(ComponentFeature.DEPLOY.getLabel().toLowerCase())
				&& featuresNode.get(ComponentFeature.DEPLOY.getLabel().toLowerCase()).asBoolean()) {
			features.addFeature(ComponentFeature.DEPLOY);
		}
	}

	public DebugStatus parseDebugStatus() {
		if (root.has(SPEC_FIELD) && root.get(SPEC_FIELD).has(DEBUG_PROCESS_ID_FIELD)) {
			String processID = root.get(SPEC_FIELD).get(DEBUG_PROCESS_ID_FIELD).asText();
			if (StringUtils.isNumeric(processID)) {
				return DebugStatus.RUNNING;
			}
		}
		return DebugStatus.UNKNOWN;
	}

	public ComponentTypeInfo parseComponentTypeInfo() {
		ComponentTypeInfo.Builder builder = new ComponentTypeInfo.Builder();
		for (JsonNode element : root) {
			if (element.has(NAME_FIELD)) {
				builder.withName(element.get(NAME_FIELD).asText());
			}
			if (element.has(STARTER_PROJECTS_FIELD)) {
				for (JsonNode starter : element.get(STARTER_PROJECTS_FIELD)) {
					builder.withStarter(parseStarter(starter));
				}
			}
		}
		return builder.build();
	}

	public Starter parseStarter(JsonNode node) {
		String name = node.asText();
		Starter.Builder builder = new Starter.Builder().withName(name);
		return builder.build();
	}

	private JsonNode resolveRefs(JsonNode node) throws IOException {
		for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
			String name = it.next();
			JsonNode child = node.get(name);
			if (child.has(DOLLAR_REF_FIELD)) {
				JsonNode replaced = resolve(root, child.get(DOLLAR_REF_FIELD).asText());
				((ObjectNode) node).set(name, replaced);
			} else {
				resolveRefs(child);
			}
		}
		return node;
	}

	private JsonNode resolve(JsonNode node, String ref) throws IOException {
		JsonNode result = node;
		String[] ids = ref.split("/");
		for (String id : ids) {
			if (!"#".equals(id)) {
				if (node.has(id)) {
					result = node.get(id);
				} else {
					throw new IOException("Can't resolved reference '" + ref + "' element " + id + " not found");
				}
			}
		}
		return result;
	}

	public ObjectNode findSchema(String crd) throws IOException {
		if (root.has(PATHS_FIELD)) {
			JsonNode node = root.get(PATHS_FIELD).get(crd);
			if (node != null && node.has(POST_FIELD) && node.get(POST_FIELD).has(PARAMETERS_FIELD)) {
				for (JsonNode parameter : node.get(POST_FIELD).get(PARAMETERS_FIELD)) {
					if (parameter.has(NAME_FIELD) && parameter.get(NAME_FIELD).asText().equals(BODY_VALUE)
							&& parameter.has(SCHEMA_FIELD)) {
						JsonNode schema = parameter.get(SCHEMA_FIELD);
						if (schema.has(DOLLAR_REF_FIELD)) {
							return (ObjectNode) resolveRefs(resolve(root, schema.get(DOLLAR_REF_FIELD).asText()));
						}
						return (ObjectNode) resolveRefs(schema);
					}
				}
			}
		}
		throw new IOException("Invalid data, no 'paths' field");
	}

	public ComponentFeatures parseComponentState() {
		ComponentFeatures state = new ComponentFeatures();
		if (root.has(RUNNING_IN_FIELD)) {
			getComponentsFeatures(state, root.get(RUNNING_IN_FIELD));
		}
		return state;
	}
}
