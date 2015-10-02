/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import org.eclipse.wst.server.core.TaskModel;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.openshift.core.connection.Connection;

import com.openshift.restclient.model.IProject;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerTaskModelAccessor {
	/* For use inside express wizard fragment */
	public static final String TASK_WIZARD_ATTR_CONNECTION = "connection";
	public static final String TASK_WIZARD_ATTR_PROJECT = "project";

	public static void set(Connection connection, TaskModel taskModel) {
		taskModel.putObject(TASK_WIZARD_ATTR_CONNECTION, connection);
	}

	public static Connection getConnection(TaskModel model) {
		return (Connection) model.getObject(TASK_WIZARD_ATTR_CONNECTION);
	}
	
	public static Connection getConnection(IServerModeUICallback callback) {
		return (Connection) callback.getAttribute(TASK_WIZARD_ATTR_CONNECTION);
	}

	public static IProject getProject(IServerModeUICallback callback) {
		return (IProject) callback.getAttribute(TASK_WIZARD_ATTR_PROJECT);
	}

}
