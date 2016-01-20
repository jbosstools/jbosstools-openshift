/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.Deployment;

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
public class OpenInWebConsoleHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IResource resource = UIUtils.getFirstElement(currentSelection, IResource.class);
		Connection connection = null;
		if (resource == null) {
			Deployment deployment = UIUtils.getFirstElement(currentSelection, Deployment.class);
			if (deployment != null) {
				resource = deployment.getService();
			}
		}
		if ( resource == null) {
			connection = UIUtils.getFirstElement(currentSelection, Connection.class);
		} else {
			connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
		}
		String msg;
		if (connection == null) {
			msg = "Could not find an OpenShift connection to open a console for";
		} else {
			String url = getWebConsoleUrl(connection, resource);
			if (!StringUtils.isEmpty(url)) {
				new BrowserUtility().checkedCreateInternalBrowser(url,
						"", OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				return Status.OK_STATUS;
			}
			msg = NLS.bind("Could not determine the url for the web console on {0}", connection.getHost());
		}
		MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "No Web Console Url", msg);
		return new Status(IStatus.WARNING, OpenShiftUIActivator.PLUGIN_ID, msg);
	}

	private String getWebConsoleUrl(Connection connection, IResource resource) {
		StringBuilder url = new StringBuilder(connection.getHost()).append("/console");
		String projectName = resource == null? null : resource.getNamespace();
		if (projectName != null) {
			url.append("/project/").append(projectName);
		}
		if (resource != null && !(resource instanceof IProject)) {
			url.append("/browse");
			//console doesn't seem to provide anchors to reach specific items
			//so we just open the root category for all given resources
			if (resource instanceof IBuildConfig ) {
				url.append("/builds/").append(resource.getName());
			} else if ( resource instanceof IBuild) {
				String buildConfig =  resource.getLabels().get("buildconfig");
				url.append("/builds/").append(buildConfig).append("/").append(resource.getName());
			} else if (resource instanceof IDeploymentConfig) {
				url.append("/deployments/").append(resource.getName());
			} else if (resource instanceof IPod) {
				url.append("/pods/").append(resource.getName());
			} else if (resource instanceof IService) {
				url.append("/services/").append(resource.getName());
			} else if (resource instanceof IImageStream) {
				url.append("/images/").append(resource.getName());
			}
		}
		return url.toString();
	}
}