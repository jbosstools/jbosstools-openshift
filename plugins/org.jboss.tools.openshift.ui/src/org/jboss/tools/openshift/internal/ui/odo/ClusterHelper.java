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
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.openshift.client.OpenShiftClient;

public class ClusterHelper {
	private static String assemble(String major, String minor) {
		return major + '.' + minor;
	}

	public static ClusterInfo getClusterInfo(KubernetesClient client) throws KubernetesClientException {
		if (client instanceof OpenShiftClient || client.isAdaptable(OpenShiftClient.class)) {
			VersionInfo oClientVersionInfo;
			VersionInfo kClientVersionInfo;
			if (client instanceof OpenShiftClient) {
				oClientVersionInfo = ((OpenShiftClient) client).getVersion();
				kClientVersionInfo = new DefaultKubernetesClient(client.getConfiguration()).getVersion();
			} else {
				oClientVersionInfo = client.adapt(OpenShiftClient.class).getVersion();
				kClientVersionInfo = client.getVersion();
			}
			return new ClusterInfo(kClientVersionInfo != null ? kClientVersionInfo.getGitVersion() : "", true,
					oClientVersionInfo != null && oClientVersionInfo.getMajor() != null
							? assemble(oClientVersionInfo.getMajor(), oClientVersionInfo.getMinor())
							: "");
		}
		VersionInfo kClientVersionInfo = client.getVersion();
		return new ClusterInfo(kClientVersionInfo != null ? kClientVersionInfo.getGitVersion() : "", false, "");
	}
}
