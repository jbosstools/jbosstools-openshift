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

import java.util.List;
import java.util.Objects;

public final class DevfileComponentType extends AbstractComponentType {
  
    private final String displayName;
    private final String description;
    private final DevfileRegistry devfileRegistry;
    private final String language;
    private final List<String> tags;  

    public DevfileComponentType(String name, String displayName, String description, DevfileRegistry devfileRegistry,
        String language, List<String> tags) {
        super(name);
        this.displayName = displayName;
        this.description = description;
        this.devfileRegistry = devfileRegistry;
        this.language = language;
        this.tags = tags;
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

    /**
     * @return the language
     */
    public String getLanguage() {
      return language;
    }

    /**
     * @return the tags
     */
    public List<String> getTags() {
      return tags;
    }
    
    public String getProjectType() {
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(devfileRegistry.getName(), getName());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof DevfileComponentType)) {
        return false;
      }
      DevfileComponentType other = (DevfileComponentType) obj;
      return Objects.equals(devfileRegistry.getName(), other.devfileRegistry.getName()) && Objects.equals(getName(), other.getName());
    }
}
