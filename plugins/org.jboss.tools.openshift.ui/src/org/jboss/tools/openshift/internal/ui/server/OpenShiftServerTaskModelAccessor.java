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

import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.jboss.tools.openshift.core.connection.Connection;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerTaskModelAccessor {

	public static final String TASK_WIZARD_ATTR_CONNECTION = "connection";
	public static final String TASK_WIZARD_ATTR_PROJECT = "project";

	public static void set(Connection connection, TaskModel model) {
		if (model == null) {
			return;
		}
		model.putObject(TASK_WIZARD_ATTR_CONNECTION, connection);
	}

	public static Connection getConnection(TaskModel model) {
		if (model == null) {
			return null;
		}
		return (Connection) model.getObject(TASK_WIZARD_ATTR_CONNECTION);
	}

	public static IServerWorkingCopy getServer(TaskModel model) {
		if (model == null) {
			return null;
		}
		return (IServerWorkingCopy) model.getObject(TaskModel.TASK_SERVER);
	}
}
