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

import java.util.concurrent.CompletableFuture;

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
  
  private CompletableFuture<String> future;
  
  private OdoCliFactory() {
  }
  
  public CompletableFuture<Odo> getOdo() {
    if (future == null) {
    	future = DownloadHelper.getInstance().downloadIfRequiredAsync("odo", OdoCli.class.getResource("/tools.json"));
    }
    return future.thenApply(command -> new OdoCli(command));
  }
}
