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

public class ComponentDescriptor {
  private final String path;
  private final String project;
  private final String application;
  private final String name;
  private final List<Integer> ports;

  ComponentDescriptor(String project, String application, String path, String name, List<Integer> ports) {
    this.project = project;
    this.application = application;
    this.path = path;
    this.name = name;
    this.ports = ports;
  }

  public String getProject() {
    return project;
  }

  public String getApplication() {
    return application;
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public List<Integer> getPorts() {
    return ports;
  }
}