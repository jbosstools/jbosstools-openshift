/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;

/**
 * @author Jeff Maury
 * 
 * A validator that accepts only selected elements being files. Prevent a folder/container/project
 * to be selected. 
 *
 */
public class FileValidator implements ISelectionStatusValidator {

    @Override
    public IStatus validate(Object[] selection) {
        if (selection.length == 0) {
            return new Status(IStatus.ERROR, OpenShiftCommonUIActivator.PLUGIN_ID, "");
        }
        for (int i= 0; i < selection.length; i++) {
            if (!(selection[i] instanceof IFile)) {
                return new Status(IStatus.ERROR, OpenShiftCommonUIActivator.PLUGIN_ID, "");
            }                   
        }
        return new Status(IStatus.OK, OpenShiftCommonUIActivator.PLUGIN_ID, "");
    }

}
