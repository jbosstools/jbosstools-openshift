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
package org.jboss.tools.openshift.core.odo.utils;

import java.io.File;
import java.io.IOException;

import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;

import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.ConfigBuilder;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;

public class ConfigHelper {
	private ConfigHelper() {
	}

	public static String getKubeConfigPath() {
		return System.getProperty("user.home") + "/.kube/config";
	}

	public static Config safeLoadKubeConfig() {
		try {
			return loadKubeConfig();
		} catch (IOException e) {
			OpenShiftCoreActivator.logError(e.getLocalizedMessage(), e);
			return null;
		}
	}

	public static Config loadKubeConfig() throws IOException {
		File f = new File(getKubeConfigPath());
		if (f.exists()) {
			return KubeConfigUtils.parseConfig(f);
		}
		return new ConfigBuilder().build();
	}

}
