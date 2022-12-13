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

import java.util.HashSet;
import java.util.Set;

public class ComponentFeatures {

	private Set<ComponentFeature> features = new HashSet<>();

	public ComponentFeatures(ComponentFeature feature) {
		addFeature(feature);
	}

	public ComponentFeatures() {
	}

	public void addFeature(ComponentFeature feature) {
		features.add(feature);
	}

	public void removeFeature(ComponentFeature feature) {
		features.remove(feature);
	}

	public void setFeatures(Set<ComponentFeature> features) {
		this.features = features;
	}

	public boolean is(ComponentFeature feature) {
		return features.contains(feature);
	}

	public boolean isDev() {
		return is(ComponentFeature.DEV);
	}

	public boolean isDeploy() {
		return is(ComponentFeature.DEPLOY);
	}

	public boolean isDebug() {
		return is(ComponentFeature.DEBUG);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		if (isDev()) {
			first = append(builder, first, ComponentFeature.DEV.getLabel());
		}
		if (isDebug()) {
			first = append(builder, first, ComponentFeature.DEBUG.getLabel());
		}
		if (isDeploy()) {
			first = append(builder, first, ComponentFeature.DEPLOY.getLabel());
		}
		return builder.toString();
	}

	private boolean append(StringBuilder builder, boolean first, String label) {
		if (!first) {
			builder.append(',');
		}
		builder.append(label);
		return false;
	}

	public boolean isOnCluster() {
		return isDev() || isDebug() || isDeploy();
	}
}
