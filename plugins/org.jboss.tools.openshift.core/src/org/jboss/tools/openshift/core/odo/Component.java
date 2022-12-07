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

public interface Component {
	String getName();

	ComponentFeatures getLiveFeatures();

	void setLiveFeatures(ComponentFeatures state);

	String getPath();

	void setPath(String path);

	default boolean hasContext() {
		return getPath() != null;
	}

	ComponentInfo getInfo();

	void setInfo(ComponentInfo componentInfo);

	class ComponentImpl implements Component {
		private final String name;
		private ComponentFeatures state;
		private String path;
		private ComponentInfo info;

		private ComponentImpl(String name, ComponentFeatures state, String path, ComponentInfo info) {
			this.name = name;
			this.state = state;
			this.path = path;
			this.info = info;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public ComponentFeatures getLiveFeatures() {
			return state;
		}

		@Override
		public void setLiveFeatures(ComponentFeatures state) {
			this.state = state;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public ComponentInfo getInfo() {
			return info;
		}

		@Override
		public void setInfo(ComponentInfo componentInfo) {
			this.info = componentInfo;
		}
	}

	static Component of(String name, ComponentFeatures state, ComponentInfo info) {
		return of(name, state, null, info);
	}

	static Component of(String name, ComponentFeatures state, String path, ComponentInfo info) {
		return new ComponentImpl(name, state, path, info);
	}
}
