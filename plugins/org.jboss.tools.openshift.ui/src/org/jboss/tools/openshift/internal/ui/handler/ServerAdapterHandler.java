/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.jboss.tools.openshift.internal.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizard;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * Command handler to select or create a server adapter from a selected Route or
 * Service.
 */
public class ServerAdapterHandler extends AbstractHandler {

	private static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		IResourceUIModel selectedResourceModel = UIUtils.getFirstElement(selection, IResourceUIModel.class);
		IResource selectedResource = (selectedResourceModel != null) 
				? selectedResourceModel.getResource() 
				: UIUtils.getFirstElement(selection, IResource.class); //Object may be selected in Properties view as IResource.
		final IServer openShiftServer = getOpenShiftServer(selectedResource);
		if (openShiftServer != null) {
			openServersView(openShiftServer, workbenchWindow);
		}
		return null;
	}

	/**
	 * Finds the OpenShift server corresponding to the selection or prompts the
	 * user to create one.
	 * 
	 * @param resource
	 *            the selected OpenShift {@link IResource}
	 * 
	 * @return the matching OpenShift {@link IServer} or <code>null</code> if
	 *         none was found or user cancelled the creation operation.
	 */
	private IServer getOpenShiftServer(final IResource resource) {
		if (resource == null) {
			return null;
		}
		
		if (resource instanceof IService) {
			final IService selectedService = (IService) resource;
			final Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(selectedService);
			return openOrCreateServerAdapter(selectedService, null, connection);
		} else if (resource instanceof IRoute) {
			final IRoute selectedRoute = (IRoute) resource;
			final IService relatedService = (IService) selectedRoute.getProject().getResources(ResourceKind.SERVICE).stream()
					.filter(s -> ResourceUtils.areRelated(selectedRoute, (IService) s))
					.findFirst()
					.orElseGet(() -> null);
			if(relatedService != null) {
				final Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(selectedRoute);
				return openOrCreateServerAdapter(relatedService, selectedRoute, connection);
			} else {
				OpenShiftUIActivator.getDefault().getLogger().logWarning("Unable to locate the service '"
						+ selectedRoute.getServiceName() + "' from route '" + selectedRoute.getName() + "'");
			}
		}
		return null;
	}

	/**
	 * Looks for an existing {@link IServer} matching the given {@code service},
	 * otherwise, opens the Server Adapter wizard to create a new one.
	 * 
	 * @param service
	 * @param route 
	 * @param connection
	 *            the OpenShift connection
	 */
	private IServer openOrCreateServerAdapter(final IService service, IRoute route, final Connection connection) {
		if (service == null || connection == null) {
			return null;
		}
		final String serviceName = OpenShiftResourceUniqueId.get(service);
		IServer server = OpenShiftServerUtils.findServerForService(serviceName);
		if (server == null) {
			server = createServer(serviceName, service, route, connection);
		}
		return server;
	}

	private IServer createServer(final String serviceName, final IService service, IRoute route, final Connection connection) {
		IServer server = null;
		try {
			IServerWorkingCopy serverWorkingCopy = OpenShiftServerUtils.create(serviceName);
			final ServerSettingsWizard serverSettingsWizard = 
					new ServerSettingsWizard(serverWorkingCopy, connection, service, route);
			if (WizardUtils.openWizardDialog(600, 650, serverSettingsWizard, Display.getDefault().getActiveShell())) {
				server = serverSettingsWizard.getCreatedServer();
			}
		} catch (CoreException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError(
					NLS.bind("Failed to create OpenShift Server Adapter for service {0}", serviceName), e);
		}
		return server;
	}
	
	private void openServersView(final IServer openShiftServer, final IWorkbenchWindow workbenchWindow) {
		try {
			final CommonNavigator serversViewPart = (CommonNavigator) workbenchWindow.getActivePage().showView(SERVERS_VIEW_ID);
			serversViewPart.setFocus();
			serversViewPart.getCommonViewer().refresh();
			serversViewPart.getCommonViewer().setSelection(new StructuredSelection(openShiftServer));
		} catch (PartInitException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Failed to open Servers View", e);
		}
	}


}
