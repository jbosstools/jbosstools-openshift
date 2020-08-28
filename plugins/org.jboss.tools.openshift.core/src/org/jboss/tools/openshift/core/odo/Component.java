/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import java.util.Objects;

public interface Component {
  String getName();
  ComponentState getState();
  void setState(ComponentState state);
  String getPath();
  void setPath(String path);
  default boolean hasContext() {
      return getPath() != null;
  }

  class ComponentImpl implements Component {
    private String name;
    private ComponentState state;
    private String path;

    private ComponentImpl(String name, ComponentState state, String path) {
      this.name = name;
      this.state = state;
      this.path = path;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public ComponentState getState() {
      return state;
    }

    @Override
    public void setState(ComponentState state) {
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
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ComponentImpl)) {
			return false;
		}
		ComponentImpl other = (ComponentImpl) obj;
		return Objects.equals(name, other.name);
	}
  }

  static Component of(String name) {
    return of(name, ComponentState.NO_CONTEXT);
  }

  static Component of(String name, ComponentState state) {
    return of(name, state, null);
  }

  static Component of(String name, ComponentState state, String path) {
    return new ComponentImpl(name, state, path);
  }
}
