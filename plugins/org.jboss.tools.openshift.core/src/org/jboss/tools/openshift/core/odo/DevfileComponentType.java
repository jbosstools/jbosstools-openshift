/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

public final class DevfileComponentType extends AbstractComponentType {
  
    private final String displayName;
    private final String description;
    private final DevfileRegistry devfileRegistry;  

    public DevfileComponentType(String name, String displayName, String description, DevfileRegistry devfileRegistry) {
        super(name);
        this.displayName = displayName;
        this.description = description;
        this.devfileRegistry = devfileRegistry;
    }

    @Override
    public ComponentKind getKind() {
        return ComponentKind.DEVFILE;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
      return displayName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
      return description;
    }

    /**
     * @return the devfileRegistry
     */
    public DevfileRegistry getDevfileRegistry() {
      return devfileRegistry;
    }
}
