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

import java.util.function.Function;

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

import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEvent;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.volume.IPersistentVolumeClaim;

/**
 * @author Fred Bricon
 * @author Jeff Maury
 */
public class OpenInWebConsoleHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IResource resource = UIUtils.getFirstElement(currentSelection, IResource.class);
		Connection connection = null;
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
				new BrowserUtility().checkedCreateExternalBrowser(url,
						OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				return Status.OK_STATUS;
			}
			msg = NLS.bind("Could not determine the url for the web console on {0}", connection.getHost());
		}
		MessageDialog.openWarning(HandlerUtil.getActiveShell(event), "No Web Console Url", msg);
		return new Status(IStatus.WARNING, OpenShiftUIActivator.PLUGIN_ID, msg);
	}

	protected String getWebConsoleUrl(Connection connection, IResource resource) {
		StringBuilder url = new StringBuilder(connection.getHost()).append("/console");
		String projectName = resource == null? null : resource.getNamespace();
		if (projectName != null) {
			url.append("/project/").append(projectName);
		}
		if (resource != null && !(resource instanceof IProject)) {
			url.append(getResourceURL(resource));
		}
		return url.toString();
	}
	
	protected String getResourceURL(IResource resource) {
		for (ResourceUrls resUrl : ResourceUrls.values()) {
			if (resUrl.getResType().isInstance(resource)) {
				return resUrl.getUrlPart() + resUrl.getEndUrlFunc().apply(resource);
			}
		}
		return "/browse";
	}
	
	protected enum ResourceUrls {
		BuildConfig			  (IBuildConfig.class, "/browse/builds/", IResource::getName),
		Build				  (IBuild.class, "/browse/builds/", r -> String.join("/", r.getLabels().get("buildconfig"), r.getName())),
		DeploymentConfig	  (IDeploymentConfig.class, "/browse/deployments/", IResource::getName),
		Pod					  (IPod.class, "/browse/pods/", IResource::getName),
		Service				  (IService.class, "/browse/services/", IResource::getName),
		ImageStream			  (IImageStream.class, "/browse/images/", IResource::getName),
		PersistentVolumeClaim (IPersistentVolumeClaim.class, "/browse/persistentvolumeclaims/", IResource::getName),
		Event                 (IEvent.class, "/browse/events/", r -> org.apache.commons.lang.StringUtils.EMPTY);
		
		private final Class<? extends IResource> resType;
		private final String urlPart;
		private final Function<IResource, String> endUrlFunc;
		private ResourceUrls(Class<? extends IResource> resType, String urlPart, Function<IResource, String> endUrlFunc) {
			this.resType = resType;
			this.urlPart = urlPart;
			this.endUrlFunc = endUrlFunc;
		}

		public Class<? extends IResource> getResType() {
			return resType;
		}

		public String getUrlPart() {
			return urlPart;
		}

		public Function<IResource, String> getEndUrlFunc() {
			return endUrlFunc;
		}
	}
}