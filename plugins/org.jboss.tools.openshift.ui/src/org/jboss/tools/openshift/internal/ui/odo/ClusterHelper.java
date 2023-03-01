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

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.openshift.client.OpenShiftClient;

public class ClusterHelper {
	public static ClusterInfo getClusterInfo(KubernetesClient client) {
		if (client instanceof OpenShiftClient) {
			return new ClusterInfo(getKubernetesVersion((OpenShiftClient) client), true,
					getOpenShiftVersion((OpenShiftClient) client));

		} else if (client.adapt(OpenShiftClient.class) != null && client.adapt(OpenShiftClient.class).isSupported()) {
			return new ClusterInfo(getKubernetesVersion(client), true, getOpenShiftVersion(client));
		} else {
			return new ClusterInfo(getKubernetesVersion(client), false, "");

		}
	}

	private static String getKubernetesVersion(OpenShiftClient client) {
		try {
			KubernetesClient kclient = new KubernetesClientBuilder().withConfig(client.getConfiguration()).build();
			return getKubernetesVersion(kclient);
		} catch (KubernetesClientException e) {
			return null;
		}
	}

	private static String getKubernetesVersion(KubernetesClient client) {
		VersionInfo version = client.getKubernetesVersion();
		return version != null ? version.getGitVersion() : "";
	}

	private static String getOpenShiftVersion(KubernetesClient client) {
		try {
			OpenShiftClient oclient = client.adapt(OpenShiftClient.class);
			return getOpenShiftVersion(oclient);
		} catch (KubernetesClientException e) {
			return null;
		}
	}

	private static String getOpenShiftVersion(OpenShiftClient client) {
		VersionInfo version = client.getVersion();
		return version != null && version.getMajor() != null ? getVersion(version.getMajor(), version.getMinor()) : "";
	}

	private static String getVersion(String major, String minor) {
		return major + '.' + minor;
	}
}
