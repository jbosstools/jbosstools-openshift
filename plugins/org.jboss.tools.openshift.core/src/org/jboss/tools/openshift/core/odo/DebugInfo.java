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

import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;

/**
 * @author Red Hat Developers
 *
 */
public class DebugInfo {
  private DebugStatus status;
  private int localPort;
  /**
   * @param notRunning
   */
  public DebugInfo(DebugStatus status) {
    setStatus(status);
  }
  
  /**
   * @return the status
   */
  public DebugStatus getStatus() {
    return status;
  }
  /**
   * @param status the status to set
   */
  public void setStatus(DebugStatus status) {
    this.status = status;
  }
  /**
   * @return the localPort
   */
  public int getLocalPort() {
    return localPort;
  }
  /**
   * @param localPort the localPort to set
   */
  public void setLocalPort(int localPort) {
    this.localPort = localPort;
  }

}
