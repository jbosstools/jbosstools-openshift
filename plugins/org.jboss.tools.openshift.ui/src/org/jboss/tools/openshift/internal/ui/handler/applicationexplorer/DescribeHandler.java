/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;

/**
 * @author Red Hat Developers
 */
public class DescribeHandler extends OdoHandler {

	@Override
	public void actionPerformed(Odo odo) throws IOException {
		try {
			if (getComponent() != null) {
				ComponentElement component = getComponent();
				odo.describeComponent(component.getParent().getWrapped(), component.getWrapped().getPath(),
						component.getWrapped().getName());
			}
		} catch (IOException e) {
			Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
					"Describe", "Describe error message:" + e.getLocalizedMessage()));
		}
	}

}
