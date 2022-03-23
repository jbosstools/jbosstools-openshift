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

public enum ComponentKind {
    DEVFILE("devfile"),
    OTHER("other");

    private final String label;

    ComponentKind(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static ComponentKind fromAnnotation(String annotation) {
        return DEVFILE;
    }
}
