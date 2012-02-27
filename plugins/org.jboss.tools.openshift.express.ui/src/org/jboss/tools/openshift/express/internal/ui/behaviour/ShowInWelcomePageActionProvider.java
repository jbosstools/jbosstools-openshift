/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.views.server.extensions.AbstractOpenBrowserServerAction;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.OpenShiftException;

/**
 * Action that shows the welcome page for a given openshift application. This is
 * actually a copy of org.jboss.ide.eclipse.as.ui.views.server.extensions.
 * ShowInWelcomePageActionProvider, should be consolidated at some point.
 * 
 * @author Andre Dietisheim
 */
public class ShowInWelcomePageActionProvider extends AbstractOpenBrowserServerAction {

	protected String getActionText() {
		return Messages.ShowInWelcomePage_Action_Text;
	}

	protected boolean shouldAddForSelection(IStructuredSelection sel) {
		IServer server = getSingleServer(sel);
		boolean shouldAddForSelection = false;
		/**
		 * TODO: move this logic to the server, allow this action to ask the
		 * server if it supports welcome page: ex. #hasWelcomePage
		 */
		if (server != null) {
			shouldAddForSelection = 
					ExpressServerUtils.isOpenShiftRuntime(server)
					&& accepts(server);
		}
		return shouldAddForSelection;
	}

	protected boolean accepts(IServer server) {
		return server.getServerState() == IServer.STATE_STARTED;
	}

	/**
	 * TODO: move this logic to the server, allow this action to ask the
	 * server for welcome page: ex. #getWelcomePage
	 */
	protected String getURL(IServer server) throws CoreException {
		if (!ExpressServerUtils.isOpenShiftRuntime(server)) {
			return null;
		}
		IApplication application = ExpressServerUtils.findApplicationForServer(server);
		if (application == null) {
			return null;
		}
		try {
			return application.getApplicationUrl();
		} catch (OpenShiftException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(
					NLS.bind("Could not get application url for server {0}", server.getName()), e);
			throw new CoreException(status);
		}
	}
}
