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
package org.jboss.tools.openshift.internal.ui.models.applicationexplorer;

import java.io.IOException;

import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.internal.common.ui.explorer.ILink;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.handler.applicationexplorer.LoginHandler;

/**
 * @author Red Hat Developers
 *
 */
public class LoginMessageElement extends MessageElement<ApplicationExplorerUIModel> implements ILink {
	
	public LoginMessageElement(ApplicationExplorerUIModel parentElement) {
		super(parentElement, "Can't connect to cluster. Click to login.");
	}

	@Override
	public void execute() {
		try {
			LoginHandler.openDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getRoot());
		} catch (IOException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(e);
		}
	}
}
