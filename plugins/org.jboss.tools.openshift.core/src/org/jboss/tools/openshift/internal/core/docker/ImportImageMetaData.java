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
import org.jboss.dmr.Property;

public class ImportImageMetaData implements IDockerImageMetadata {

	private static final String[] ROOT = new String[] { "image", "dockerImageMetadata", "ContainerConfig" };
	private static final String[] PORTS = (String[]) ArrayUtils.add(ROOT, "ExposedPorts");
	private static final String[] ENV = (String[]) ArrayUtils.add(ROOT, "Env");
	private static final String[] LABELS = (String[]) ArrayUtils.add(ROOT, "Labels");
	private static final String[] VOLUMES = (String[]) ArrayUtils.add(ROOT, "Volumes");

	private final ModelNode node;

	public ImportImageMetaData(final String json) {
		this.node = ModelNode.fromJSONString(json);
	}

	@Override
	public Set<String> exposedPorts() {
		ModelNode ports = node.get(PORTS);
		if (ports.isDefined()) {
			return ports.keys();
		}
		return Collections.emptySet();
	}

	@Override
	public List<String> env() {
		ModelNode env = node.get(ENV);
		if (env.isDefined()) {
			return env.asList().stream().map(n -> n.asString()).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public Map<String, String> labels() {
		ModelNode labels = node.get(LABELS);
		if (labels.isDefined()) {
			return labels.asPropertyList().stream()
					.collect(Collectors.toMap(Property::getName, p -> p.getValue().asString()));
		}
		return Collections.emptyMap();
	}

	@Override
	public Set<String> volumes() {
		ModelNode volumes = node.get(VOLUMES);
		if (volumes.isDefined()) {
			return volumes.keys();
		}
		return Collections.emptySet();
	}
}