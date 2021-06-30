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
package org.jboss.tools.openshift.internal.ui.odo;
public class ClusterInfo {
    private final String kubernetesVersion;
    private final boolean openshift;
    private final String openshiftVersion;

    ClusterInfo(String kubernetesVersion, boolean openshift, String openshiftVersion) {
        this.kubernetesVersion = kubernetesVersion;
        this.openshift = openshift;
        this.openshiftVersion = openshiftVersion;
    }

    public String getKubernetesVersion() {
        return kubernetesVersion;
    }

    public boolean isOpenshift() {
        return openshift;
    }

    public String getOpenshiftVersion() {
        return openshiftVersion;
    }
}