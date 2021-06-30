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

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.openshift.client.OpenShiftClient;

public class ClusterHelper {
    private static String assemble(String major, String minor) {
        return major + '.' + minor;
    }

    public static ClusterInfo getClusterInfo(KubernetesClient client) {
        if (client instanceof OpenShiftClient || client.isAdaptable(OpenShiftClient.class)) {
            OpenShiftClient oclient;
            if (client instanceof OpenShiftClient) {
                oclient = (OpenShiftClient) client;
                client = new DefaultKubernetesClient(client.getConfiguration());
            } else {
                oclient = client.adapt(OpenShiftClient.class);
            }
            VersionInfo oVersion = oclient.getVersion();
            return new ClusterInfo(client.getVersion().getGitVersion(), true,
                    oVersion != null && oVersion.getMajor() != null? assemble(oVersion.getMajor(), oVersion.getMinor()) : "");
        } else {
            return new ClusterInfo(client.getVersion().getGitVersion(), false, "");
        }
    }
}
