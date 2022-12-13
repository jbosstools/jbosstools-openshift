/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ComponentFeature {
	DEV("dev", "Watching for changes in the current directory", Collections.singletonList("dev")) {
		@Override
		public ComponentFeature getPeer() {
			return DEBUG;
		}
	},
	DEBUG("debug", "Watching for changes in the current directory", Arrays.asList("dev", "--debug")) {
		@Override
		public ComponentFeature getPeer() {
			return DEV;
		}
	},
	DEPLOY("deploy", "Your Devfile has been successfully deployed", Collections.singletonList("deploy"),
			Arrays.asList("delete", "component", "-f"));

	private final String label;

	private final String output;

	private final List<String> startArgs;

	private final List<String> stopArgs;

	private ComponentFeature(String label, String output, List<String> startArgs, List<String> stopArgs) {
		this.label = label;
		this.output = output;
		this.startArgs = startArgs;
		this.stopArgs = stopArgs;
	}

	private ComponentFeature(String label, String output, List<String> startArgs) {
		this(label, output, startArgs, Collections.emptyList());
	}

	public ComponentFeature getPeer() {
		return null;
	}

	public static ComponentFeature fromLabel(String label) {
		for (ComponentFeature feature : values()) {
			if (feature.label.equalsIgnoreCase(label)) {
				return feature;
			}
		}
		return null;
	}

	public String getLabel() {
		return label;
	}

	public List<String> getStartArgs() {
		return startArgs;
	}

	public List<String> getStopArgs() {
		return stopArgs;
	}

	public CharSequence getOutput() {
		return output;
	}
}
