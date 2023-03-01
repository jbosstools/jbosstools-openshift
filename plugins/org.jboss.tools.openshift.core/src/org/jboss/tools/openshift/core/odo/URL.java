/*******************************************************************************
 * Copyright (c) 2019-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

public interface URL {
	  String getName();

	  String getProtocol();

	  String getHost();

	  String getPath();

	  String getLocalPort();

	  String getContainerPort();

	  static URL of(String name, String host, String localPort, String containerPort, String path) {
	    return new URL() {
	      @Override
	      public String getName() {
	        return name;
	      }

	      @Override
	      public String getHost() {
	        return host;
	      }

	      @Override
	      public String getPath() {
	        return path;
	      }

	      @Override
	      public String getProtocol() {
	        return "http";
	      }

	      @Override
	      public String getLocalPort() {
	        return localPort;
	      }

	      @Override
	      public String getContainerPort() {
	        return containerPort;
	      }
	    };
	  }

	  static URL of(String name, String host, String localPort, String containerPort) {
	    return of(name, host, localPort, containerPort, "/");
	  }

	  default String asURL() {
	    return "http://" + getHost() + ":" + getLocalPort() + getPath();
	  }
	}
