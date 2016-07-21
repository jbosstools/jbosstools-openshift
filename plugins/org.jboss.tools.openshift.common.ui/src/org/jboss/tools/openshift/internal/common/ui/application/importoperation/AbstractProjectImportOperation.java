/*******************************************************************************
 * Copyright (c) 2011-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.application.importoperation;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * @author André Dietisheim <adietish@redhat.com>
 * @author Jeff Maury
 */
public class AbstractProjectImportOperation {

	private File projectFolder;

	public AbstractProjectImportOperation(File projectDirectory) {
		this.projectFolder = projectDirectory;
	}

	protected File getProjectDirectory() {
		return projectFolder;
	}

	protected boolean isReadable(File destination) {
		return destination != null
				&& destination.exists()
				&& destination.canRead();
	}

	/**
	 * Display a warning message about projects to be overwritten.
	 * 
	 * @param title the dialog title
	 * @param message the dialog message
	 * @return true if projects should overwritten
	 */
    protected boolean displayOverwriteDialog(final String title, final String message) {
        final boolean[] overwrite = new boolean[1];
                
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                overwrite[0] = MessageDialog.openQuestion(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                        title,
                        message);
            }

        });
        return overwrite[0];
    }
}