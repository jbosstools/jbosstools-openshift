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

public class S2iComponentType extends AbstractComponentType {

    private final List<String> versions;

    public S2iComponentType(String name, List<String> versions){
        super(name);
        this.versions = versions;
    }

    public List<String> getVersions() {
        return versions;
    }

    @Override
    public ComponentKind getKind() {
        return ComponentKind.S2I;
    }
}
