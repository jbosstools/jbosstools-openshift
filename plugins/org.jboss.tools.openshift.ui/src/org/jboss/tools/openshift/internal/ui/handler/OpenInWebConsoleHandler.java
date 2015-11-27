/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Fred Bricon
 */
public class OpenInWebConsoleHandler extends OpenInWebBrowserHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IResource resource = UIUtils.getFirstElement(currentSelection, IResource.class);
		Connection connection = null;
		if ( resource == null) {
			connection = UIUtils.getFirstElement(currentSelection, Connection.class);
		} else {
			connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
		}
		if (connection != null) {
			String url = getWebConsoleUrl(connection, resource);
			openInBrowser(HandlerUtil.getActiveShell(event) , url);
		}
		return null;
	}

	private String getWebConsoleUrl(Connection connection, IResource resource) {
		StringBuilder url = new StringBuilder(connection.getHost()).append("/console");
		IProject project = resource == null? null : resource.getProject();
		if (project != null) {
			url.append("/project/").append(project.getName());
		}
		if (resource != null && !(resource instanceof IProject)) {
			url.append("/browse");
			//console doesn't seem to provide anchors to reach specific items
			//so we just open the root category for all given resources
			if (resource instanceof IBuildConfig || resource instanceof IBuild) {
				url.append("/builds");
			} else if (resource instanceof IDeploymentConfig) {
				url.append("/deployments");
			} else if (resource instanceof IPod) {
				url.append("/pods");
			} else if (resource instanceof IService) {
				url.append("/services");
			} else if (resource instanceof IImageStream) {
				url.append("/images");
			}
		}
		return url.toString();
	}
}