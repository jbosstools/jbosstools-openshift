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

public interface ComponentMetadata {
	String getRegistry();

	String getComponentType();

	public static ComponentMetadata of(String registry, String componentType) {
		return new ComponentMetadata() {
			@Override
			public String getRegistry() {
				return registry;
			}

			@Override
			public String getComponentType() {
				return componentType;
			}
		};
	}
}
