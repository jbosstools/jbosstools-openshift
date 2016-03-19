/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.explorer;

import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.explorer.ILink;
import org.jboss.tools.openshift.internal.ui.handler.NewProjectHandler;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class NewProjectLinkNode implements ILink {
	private Connection connection;

	public NewProjectLinkNode(Connection connection) {
		this.connection = connection;
	}

	@Override
	public String toString() {
		return "No projects are available. Click this link to create a new project...";
	}

	@Override
	public void execute() {
		NewProjectHandler.openNewProjectDialog(connection, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());		
	}
}
