/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

/**
 * @author Red Hat Developers
 *
 */
public interface Starter {
  String getName();
  String getDescription();
  
  public class Builder {
    private String name;
    private String description;
    
    public Builder withName(String name) {
      this.name = name;
      return this;
    }
    
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }
    
    public Starter build() {
      return new Starter() {

        @Override
        public String getName() {
          return name;
        }

        @Override
        public String getDescription() {
          return description;
        }
      };
    }
  }
}
