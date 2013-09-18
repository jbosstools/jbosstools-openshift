/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.wst.server.core.TaskModel;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.openshift.express.internal.core.behaviour.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;

public class BehaviorTaskModelUtil {
	/* For use inside express wizard fragment */
	public static final String TASK_WIZARD_ATTR_CONNECTION = "connection";
	public static final String TASK_WIZARD_ATTR_DOMAIN = "domain";
	public static final String TASK_WIZARD_ATTR_APP_LIST = "appList";
	public static final String TASK_WIZARD_ATTR_SELECTED_APP = "application";

	public static void put(Connection connection, TaskModel taskModel) {
		taskModel.putObject(TASK_WIZARD_ATTR_CONNECTION, connection);
	}

	public static Connection getConnection(IServerModeUICallback callback) {
		return (Connection) callback.getAttribute(TASK_WIZARD_ATTR_CONNECTION);
	}

	public static void put(IDomain domain, TaskModel taskModel) {
		taskModel.putObject(TASK_WIZARD_ATTR_DOMAIN, domain);
	}

	public static IDomain getDomain(IServerModeUICallback callback) {
		return (IDomain) callback.getAttribute(TASK_WIZARD_ATTR_DOMAIN);
	}

	public static void put(IApplication application, TaskModel taskModel) {
		taskModel.putObject(OpenShiftServerUtils.TASK_WIZARD_ATTR_SELECTED_APP, application);
	}
	
	public static IApplication getApplication(IServerModeUICallback callback) {
		return (IApplication) callback.getAttribute(OpenShiftServerUtils.TASK_WIZARD_ATTR_SELECTED_APP);
	}

}
