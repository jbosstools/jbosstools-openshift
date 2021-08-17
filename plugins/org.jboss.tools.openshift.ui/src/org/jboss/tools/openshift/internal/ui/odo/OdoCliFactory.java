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

import java.io.IOException;

import org.jboss.tools.common.util.DownloadHelper;
import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class OdoCliFactory {
  
  private static OdoCliFactory INSTANCE;
  
  public static OdoCliFactory getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new OdoCliFactory();
    }
    return INSTANCE;
  }
  
  private String command;
  
  private OdoCliFactory() {
  }
  
  public Odo getOdo() {
    if (command == null) {
      try {
        command = getCommand();
      } catch (IOException e) {
      }
    }
    return new OdoCli(command);
  }
  
  private String getCommand() throws IOException {
    if (command == null) {
      command = getOdoCommand();
    }
    return command;
  }

  private String getOdoCommand() throws IOException {
    return DownloadHelper.getInstance().downloadIfRequired("odo", OdoCli.class.getResource("/tools.json"));
  }


}
