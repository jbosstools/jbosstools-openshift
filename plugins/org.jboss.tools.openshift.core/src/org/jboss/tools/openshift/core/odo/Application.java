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

public interface Application {
  String getName();

  static Application of(String name) {
    return new ApplicationImpl(name);
  }
  
  class ApplicationImpl implements Application {

	private final String name;
	
	private ApplicationImpl(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
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
		if (!(obj instanceof ApplicationImpl)) {
			return false;
		}
		ApplicationImpl other = (ApplicationImpl) obj;
		return Objects.equals(name, other.name);
	}
  }
}
