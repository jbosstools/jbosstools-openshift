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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public final class KubernetesLabels {
  public static final String APP_LABEL = "app.kubernetes.io/part-of";

  public static final String COMPONENT_NAME_LABEL = "app.kubernetes.io/instance";

  /**
   * Labels used for components prior to odo 1.0
   */
  public static final String COMPONENT_NAME_LABEL_PRE10 = "app.kubernetes.io/component-name";

  public static final String COMPONENT_TYPE_LABEL = "app.kubernetes.io/component-type";

  public static final String COMPONENT_VERSION_LABEL = "app.kubernetes.io/component-version";

  public static final String RUNTIME_NAME_LABEL = "app.kubernetes.io/name";

  public static final String RUNTIME_VERSION_LABEL = "app.openshift.io/runtime-version";

  public static final String NAME_LABEL = "app.kubernetes.io/name";

  public static final String URL_NAME_LABEL = "app.kubernetes.io/url-name";

  public static final String ODO_URL_NAME = "odo.openshift.io/url-name";

  public static final String STORAGE_NAME_LABEL = "app.kubernetes.io/storage-name";

  public static final String ODO_MIGRATED_LABEL = "odo.openshift.io/migrated";

  /*
   * Annotations
   */
  public static final String COMPONENT_SOURCE_TYPE_ANNOTATION = "app.kubernetes.io/component-source-type";

  public static final String VCS_URI_ANNOTATION = "app.openshift.io/vcs-uri";

  public static String getComponentName(HasMetadata resource) {
    ObjectMeta metadata = resource.getMetadata();
    if (metadata.getLabels() != null) {
      return metadata.getLabels().computeIfAbsent(COMPONENT_NAME_LABEL,
              key -> metadata.getName());
    } else {
      return metadata.getName();
    }
  }
}
