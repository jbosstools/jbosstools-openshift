/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.docker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

public class ImageStreamTagMetaData implements IDockerImageMetadata {

	private static final String[] ROOT = new String[] { "image", "dockerImageMetadata" };
	private static final String CONTAINER_CONFIG = "ContainerConfig";
	private static final String CONFIG = "Config";
	private static final String ENV = "Env";
	private static final String EXPOSED_PORT = "ExposedPorts";
	private static final String VOLUMES = "Volumes";
	private static final String LABELS = "Labels";

	private final ModelNode node;
	private final String[] CONFIG_ROOT;
	private final String[] PORT_KEY;
	private final String[] VOLUMES_KEY;
	private final String[] ENV_KEY;
	private final String[] LABELS_KEY;

	public ImageStreamTagMetaData(final String json) {
		this.node = ModelNode.fromJSONString(json);
		final ModelNode config = this.node.get(ROOT).get(CONTAINER_CONFIG);
		if (ModelType.OBJECT == config.getType() && config.has(EXPOSED_PORT)) {
			CONFIG_ROOT = (String[]) ArrayUtils.add(ROOT, CONTAINER_CONFIG);
		} else {
			CONFIG_ROOT = (String[]) ArrayUtils.add(ROOT, CONFIG);
		}
		PORT_KEY = (String[]) ArrayUtils.add(CONFIG_ROOT, EXPOSED_PORT);
		VOLUMES_KEY = (String[]) ArrayUtils.add(CONFIG_ROOT, VOLUMES);
		ENV_KEY = (String[]) ArrayUtils.add(CONFIG_ROOT, ENV);
		LABELS_KEY = (String[]) ArrayUtils.add(CONFIG_ROOT, LABELS);
	}

	@Override
	public Set<String> exposedPorts() {
		ModelNode ports = node.get(PORT_KEY);
		if (ports.isDefined()) {
			return ports.keys();
		}
		return Collections.emptySet();
	}

	@Override
	public List<String> env() {
		ModelNode env = node.get(ENV_KEY);
		if (env.isDefined()) {
			return env.asList().stream().map(n -> n.asString()).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public Map<String, String> labels() {
		ModelNode labels = node.get(LABELS_KEY);
		if (labels.isDefined()) {
			return labels.asPropertyList().stream()
					.collect(Collectors.toMap(Property::getName, p -> p.getValue().asString()));
		}
		return Collections.emptyMap();
	}

	@Override
	public Set<String> volumes() {
		ModelNode volumes = node.get(VOLUMES_KEY);
		if (volumes.isDefined()) {
			return volumes.keys();
		}
		return Collections.emptySet();
	}
}