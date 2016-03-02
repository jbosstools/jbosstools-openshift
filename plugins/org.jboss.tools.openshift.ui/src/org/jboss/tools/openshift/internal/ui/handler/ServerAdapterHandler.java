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

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.core.server.OpenShiftServer;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.core.util.OpenShiftResourceUniqueId;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizard;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * Command handler to select or create a server adapter from a selected Route or
 * Service.
 */
public class ServerAdapterHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow workbenchWindow = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow();
		final IStructuredSelection selection = (IStructuredSelection) workbenchWindow
				.getSelectionService().getSelection();
		final IResourceUIModel selectedResource = UIUtils.getFirstElement(selection, IResourceUIModel.class);
		final IServer openShiftServer = getOpenShiftServer(selectedResource);
		if (openShiftServer != null) {
			try {
				final CommonNavigator serversViewPart = (CommonNavigator) workbenchWindow.getActivePage().showView("org.eclipse.wst.server.ui.ServersView");
				serversViewPart.setFocus();
				serversViewPart.getCommonViewer().refresh();
				serversViewPart.getCommonViewer().setSelection(new StructuredSelection(openShiftServer));
			} catch (PartInitException e) {
				OpenShiftUIActivator.getDefault().getLogger().logError("Failed to open Servers View", e);
			}
		}
		return null;
	}

	/**
	 * Finds the OpenShift server corresponding to the selection or prompts the
	 * user to create one.
	 * 
	 * @param selectedResource
	 *            the selected OpenShift {@link IResourceUIModel}
	 * 
	 * @return the matching OpenShift {@link IServer} or <code>null</code> if
	 *         none was found or user cancelled the creation operation.
	 */
	private IServer getOpenShiftServer(final IResourceUIModel selectedResource) {
		if (selectedResource != null) {
			if (selectedResource.getResource() instanceof IService) {
				final IService selectedService = (IService) selectedResource.getResource();
				final Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(selectedService);
				return openOrCreateServerAdapter(selectedService, connection);
			} else if (selectedResource.getResource() instanceof IRoute) {
				final IRoute selectedRoute = (IRoute) selectedResource.getResource();
				final Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(selectedRoute);
				final IService relatedService = selectedRoute.getProject().getResources(ResourceKind.SERVICE).stream()
						.map(resource -> (IService) resource)
						.filter(service -> service.getName().equals(selectedRoute.getServiceName())).findFirst()
						.orElseGet(() -> null);
				if(relatedService != null) {
					return openOrCreateServerAdapter(relatedService, connection);
				} else {
					OpenShiftUIActivator.getDefault().getLogger().logWarning("Unable to locate the service '"
							+ selectedRoute.getServiceName() + "' from route '" + selectedRoute.getName() + "'");
				}
			}
		}
		return null;
	}

	/**
	 * Looks for an existing {@link IServer} matching the given {@code service},
	 * otherwise, opens the Server Adapter wizard to create a new one.
	 * 
	 * @param serviceName
	 *            the name of the OpenShift {@link IService}
	 * @param connection
	 *            the OpenShift connection
	 */
	private IServer openOrCreateServerAdapter(final IService service, final Connection connection) {
		if (service == null || connection == null) {
			return null;
		}
		final String serviceName = OpenShiftResourceUniqueId.get(service);
		final IServerType openShiftServerType = ServerCore.findServerType(OpenShiftServer.SERVER_TYPE_ID);
		final Optional<IServer> match = Stream.of(ServerCore.getServers())
				.filter(server -> server.getServerType().equals(openShiftServerType)
						&& server.getAttribute(OpenShiftServerUtils.ATTR_SERVICE, "").equals(serviceName))
				.findAny();
		if (match.isPresent()) {
			return match.get();
		}
		// prompt the user to create a new Server Adapter.
		try {
			final IServerWorkingCopy swc = (IServerWorkingCopy) openShiftServerType.createServer(serviceName, null,
					null);
			final ServerSettingsWizard serverSettingsWizard = new ServerSettingsWizard(swc, connection, service);
			final WizardDialog wizardDialog = new WizardDialog(Display.getDefault().getActiveShell(),
					serverSettingsWizard);
			wizardDialog.setPageSize(600, 650);
			wizardDialog.create();
			if (wizardDialog.open() == Window.OK) {
				return serverSettingsWizard.getCreatedServer();
			}
		} catch (CoreException e) {
			OpenShiftUIActivator.getDefault().getLogger()
					.logError("Failed to create OpenShift Server Adapter for service '" + serviceName + "'", e);
		}
		return null;
	}

}
