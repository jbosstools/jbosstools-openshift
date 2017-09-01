/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.js.server.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ISubsystemController;
import org.jboss.tools.openshift.core.server.behavior.OpenShiftPublishController;
import org.jboss.tools.openshift.internal.js.storage.SessionStorage;

public class OpenShiftNodejsPublishController extends OpenShiftPublishController implements ISubsystemController {

    public OpenShiftNodejsPublishController() {
        super();
    }

    @Override
    public void publishStart(final IProgressMonitor monitor) throws CoreException {
        syncDownFailed = false;
        if (SessionStorage.get().containsKey(getServer())) {
            return;
        }
        super.publishStart(monitor);
    }

}
