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

public enum ComponentSourceType {
    LOCAL("Local", "local"),
    GIT("Git", "git"),
    BINARY("Binary", "binary");

    private final String label;
    private final String annotation;

    ComponentSourceType(String label, String annotation) {
        this.label = label;
        this.annotation = annotation;
    }

    @Override
    public String toString() {
        return label;
    }

    public static ComponentSourceType fromAnnotation(String annotation) {
        for(ComponentSourceType sourceType : values()) {
            if (sourceType.annotation.equals(annotation)) {
                return sourceType;
            }
        }
        return LOCAL;
    }
}
