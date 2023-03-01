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

import java.util.List;

public interface Binding {
	String getName();

	Service getService();

	List<String> getEnvironmentVariables();

	static Binding of(String name, Service service, List<String> bindingEnvVars) {
		return new Binding() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public Service getService() {
				return service;
			}

			@Override
			public List<String> getEnvironmentVariables() {
				return bindingEnvVars;
			}
		};
	}
}
