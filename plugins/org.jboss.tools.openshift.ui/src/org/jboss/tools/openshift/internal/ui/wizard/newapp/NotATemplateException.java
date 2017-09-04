/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

public class NotATemplateException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    String resourceKind;

    public NotATemplateException(String resourceKind) {
        super("Wrong resource kind: " + resourceKind);
        this.resourceKind = resourceKind;
    }

    public String getResourceKind() {
        return resourceKind;
    }
}
