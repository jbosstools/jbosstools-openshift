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

import java.util.HashMap;
import java.util.Map;

public interface ComponentInfo {
    ComponentSourceType getSourceType();
    String getComponentTypeName();
    String getComponentTypeVersion();
    String getRepositoryURL();
    String getRepositoryReference();
    String getBinaryURL();
    boolean isMigrated();
    ComponentKind getComponentKind();
    Map<String, String> getEnv();

    public class Builder {
        private ComponentSourceType sourceType;
        private String componentTypeName;
        private String componentTypeVersion;
        private String repositoryURL;
        private String repositoryReference;
        private String binaryURL;
        private boolean migrated;
        private ComponentKind kind;
        private Map<String, String> env = new HashMap<>();

        public Builder withSourceType(ComponentSourceType sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder withComponentTypeName(String componentTypeName) {
            this.componentTypeName= componentTypeName;
            return this;
        }

        public Builder withComponentTypeVersion(String componentTypeVersion) {
            this.componentTypeVersion = componentTypeVersion;
            return this;
        }

        public Builder withRepositoryURL(String repositoryURL) {
            this.repositoryURL = repositoryURL;
            return this;
        }

        public Builder withRepositoryReference(String repositoryReference) {
            this.repositoryReference = repositoryReference;
            return this;
        }

        public Builder withBinaryURL(String binaryURL) {
            this.binaryURL = binaryURL;
            return this;
        }

        public Builder withMigrated(boolean migrated) {
            this.migrated = migrated;
            return this;
        }
        
        public Builder withComponentKind(ComponentKind kind) {
            this.kind = kind;
            return this;
        }
        
        public Builder withEnv(Map<String, String> env) {
          this.env = env;
          return this;
        }
        
        public Builder addEnv(String key, String val) {
          env.put(key, val);
          return this;
        }

        public ComponentInfo build() {
            return new ComponentInfo() {
                @Override
                public ComponentSourceType getSourceType() {
                    return sourceType;
                }

                @Override
                public String getComponentTypeName() {
                    return componentTypeName;
                }

                @Override
                public String getComponentTypeVersion() {
                    return componentTypeVersion;
                }

                @Override
                public String getRepositoryURL() {
                    return repositoryURL;
                }

                @Override
                public String getRepositoryReference() {
                    return repositoryReference;
                }

                @Override
                public String getBinaryURL() {
                    return binaryURL;
                }

                @Override
                public boolean isMigrated() {
                    return migrated;
                }

                @Override
                public ComponentKind getComponentKind() {
                  return kind;
                }

                @Override
                public Map<String, String> getEnv() {
                  return env;
                }
            };
        }
    }
}
