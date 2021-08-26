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

public interface Service {
    String getName();
    String getApiVersion();
    String getKind();

    static Service of(String name, String apiVersion, String kind) {
        return new Service() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getApiVersion() {
                return apiVersion;
            }

            @Override
            public String getKind() {
                return kind;
            }
        };
    }
}
