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

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;

public interface Storage {
    public String getName();
    public String getSize();
    public String getPath();

    static Storage of(String name) {
        return new Storage() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getSize() {
                return "";
            }

            @Override
            public String getPath() {
                return "";
            }
        };
    }

    public static String getStorageName(PersistentVolumeClaim pvc) {
        String res = null;
        if (pvc.getMetadata().getLabels() != null) {
            res = pvc.getMetadata().getLabels().get(KubernetesLabels.STORAGE_NAME_LABEL);
        }
        if (res == null) {
            res = KubernetesLabels.getComponentName(pvc);
        }
        return res;
    }

    /**
	 * @return the list of available sizes
	 */
	public static List<String> getSizes() {
		return Arrays.asList("1Gi", "1.5Gi", "2Gi");
	}
}
