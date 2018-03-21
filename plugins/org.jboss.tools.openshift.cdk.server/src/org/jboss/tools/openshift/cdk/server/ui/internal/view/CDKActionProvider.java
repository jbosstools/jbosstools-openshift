/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.ui.internal.view;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.internal.cnf.ServerActionProvider;
import org.eclipse.wst.server.ui.internal.view.servers.AbstractServerAction;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKDockerUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ServiceManagerEnvironment;
import org.jboss.tools.openshift.internal.ui.models.OpenshiftUIModel;

public class CDKActionProvider extends CommonActionProvider {
	private ICommonActionExtensionSite actionSite;
	private ShowInViewAfterStartupAction showInOpenshiftViewAction;
	private ShowInViewAfterStartupAction showInDockerViewAction;
	private SetupCDKAction setupCDKAction;

	private static final String DOCKER_VIEW_ID = "org.eclipse.linuxtools.docker.ui.dockerExplorerView";
	private static final String OPENSHIFT_VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";

	public CDKActionProvider() {
		super();
	}

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		this.actionSite = aSite;
		createActions(aSite);
	}

	protected void createActions(ICommonActionExtensionSite aSite) {
		ICommonViewerSite site = aSite.getViewSite();
		if (site instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
			showInOpenshiftViewAction = new ShowInOpenshiftViewAfterStartupAction(wsSite.getSelectionProvider(),
					OPENSHIFT_VIEW_ID);
			showInDockerViewAction = new ShowInDockerViewAfterStartupAction(wsSite.getSelectionProvider(),
					DOCKER_VIEW_ID);
			setupCDKAction = new SetupCDKAction(wsSite.getSelectionProvider());
		}
	}

	private class SetupCDKAction extends Action {
		private ISelectionProvider sp;

		public SetupCDKAction(ISelectionProvider sp) {
			super("Setup CDK");
			this.sp = sp;
		}

		@Override
		public void run() {
			IServer s = getServerFromSelection();
			if (s != null && shouldRun(s)) {
				run2(s);
			}
		}

		public boolean shouldRun() {
			IServer s = getServerFromSelection();
			return (s != null && shouldRun(s));
		}

		private void run2(IServer server) {
			CDK3Server cdk3 = (CDK3Server) server.loadAdapter(CDK3Server.class, new NullProgressMonitor());
			if (cdk3 != null) {
				String home = cdk3.getMinishiftHome();
				if( new File(home).exists()) {
					Shell s = actionSite.getViewSite().getShell();
					MessageBox messageBox = new MessageBox(s,SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
					
					String msgText = "Setup CDK will delete all existing contents of {0}. Are you sure you want to continue?";
					String msg = NLS.bind(msgText, home);
					messageBox.setMessage(msg);
					messageBox.setText("Warning: Folder already exists!");
					
					int ret = messageBox.open();
					if( ret != SWT.OK) {
						return;
					}
				}
			}
			
			
			String cmd = MinishiftBinaryUtility.getMinishiftLocation(server);
			String args = "setup-cdk --force";
			try {
				ILaunchConfiguration lc = server.getLaunchConfiguration(true, new NullProgressMonitor());
				ILaunchConfigurationWorkingCopy lc2 = new CDKLaunchUtility().createExternalToolsLaunch(server, args,
						new Path(cmd).lastSegment(), lc, cmd, true);
				lc2.launch("run", new NullProgressMonitor());
			} catch (CoreException ce) {
				CDKCoreActivator.pluginLog().logError(ce);
			}
		}

		private boolean shouldRun(IServer server) {
			String typeId = server.getServerType().getId();
			List<String> types = Arrays.asList(CDK3Server.MINISHIFT_BASED_CDKS);
			return types.contains(typeId) && !isCDKInitialized(server);
		}

		private IServer getServerFromSelection() {
			IStructuredSelection selection = (IStructuredSelection) sp.getSelection();
			if (selection.getFirstElement() instanceof IServer) {
				return (IServer) selection.getFirstElement();
			}
			return null;
		}
	}

	private static class ShowInOpenshiftViewAfterStartupAction extends ShowInViewAfterStartupAction {
		public ShowInOpenshiftViewAfterStartupAction(ISelectionProvider sp, String viewId) {
			super(sp, viewId);
		}

		@Override
		protected Object adaptToViewItem(IServer server) {
			ControllableServerBehavior beh = (ControllableServerBehavior) server
					.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
			ServiceManagerEnvironment adb = null;
			if (beh != null) {
				adb = (ServiceManagerEnvironment) beh.getSharedData(ServiceManagerEnvironment.SHARED_INFO_KEY);
			}

			if (adb != null) {
				return OpenshiftUIModel.getInstance().getConnectionWrapperForConnection(
						new CDKOpenshiftUtility().findExistingOpenshiftConnection(server, adb));
			}
			return null;
		}
	}

	private static class ShowInDockerViewAfterStartupAction extends ShowInViewAfterStartupAction {
		public ShowInDockerViewAfterStartupAction(ISelectionProvider sp, String viewId) {
			super(sp, viewId);
		}

		@Override
		protected Object adaptToViewItem(IServer server) {
			if (server != null)
				return new CDKDockerUtility().findDockerConnection(server.getName());
			return null;
		}

	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		ICommonViewerSite site = actionSite.getViewSite();
		IStructuredSelection selection = null;
		if (site instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
			selection = (IStructuredSelection) wsSite.getSelectionProvider().getSelection();
		}

		IContributionItem quick = menu.find("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
		if (quick != null && selection != null && selection.size() == 1) {
			if (selection.getFirstElement() instanceof IServer) {
				IServer server = (IServer) selection.getFirstElement();
				if (acceptsServer(server)) {
					if (menu instanceof MenuManager) {
						((MenuManager) quick).add(showInDockerViewAction);
						((MenuManager) quick).add(showInOpenshiftViewAction);
					}
				}
				if (setupCDKAction.shouldRun()) {
					menu.insertBefore(ServerActionProvider.TOP_SECTION_END_SEPARATOR, setupCDKAction);
				}
			}
		}
	}

	private boolean isCDKInitialized(IServer server) {
		CDK3Server cdk3 = (CDK3Server) server.loadAdapter(CDK3Server.class, new NullProgressMonitor());
		if (cdk3 != null) {
			String home = cdk3.getMinishiftHome();
			File homeF = new File(home);
			if (homeF.exists() && homeF.isDirectory()) {
				File cdk = new File(homeF, "cdk");
				File config = new File(homeF, "config");
				File cache = new File(homeF, "cache");
				File configJSON = new File(config, "config.json");
				if (cdk.exists() && config.exists() && cache.exists() && configJSON.exists()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean acceptsServer(IServer s) {
		// For now lets just do cdk servers, but we can change this if we wanted it
		// extensible via an adapter?
		if (s != null && s.getServerType() != null) {
			String typeId = s.getServerType().getId();
			if (CDKServer.CDK_SERVER_TYPE.equals(typeId) || CDKServer.CDK_V3_SERVER_TYPE.equals(typeId)) {
				return true;
			}
		}
		return false;
	}

	private abstract static class ShowInViewAfterStartupAction extends AbstractServerAction {
		private IStructuredSelection previousSelection;
		private IServerListener serverListener;
		private String viewId;

		public ShowInViewAfterStartupAction(ISelectionProvider sp, String viewId) {
			super(sp, null);
			this.viewId = viewId;

			IViewRegistry reg = PlatformUI.getWorkbench().getViewRegistry();
			IViewDescriptor desc = reg.find(viewId);
			setText(desc.getLabel());
			setImageDescriptor(desc.getImageDescriptor());
			serverListener = new IServerListener() {
				@Override
				public void serverChanged(final ServerEvent event) {
					// If this is the server that was / is selected
					if (previousSelection != null && previousSelection.size() > 0
							&& previousSelection.getFirstElement().equals(event.getServer())) {
						// and it switches state, update enablement
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								if (UnitedServerListener.serverSwitchesToState(event, IServer.STATE_STARTED)) {
									setEnabled(true);
								} else if (UnitedServerListener.serverSwitchesToState(event, IServer.STATE_STOPPED)) {
									setEnabled(false);
								}
							}
						});
					}
				}
			};
			selectionChanged(sp.getSelection());
		}

		protected abstract Object adaptToViewItem(IServer server);

		@Override
		public boolean accept(IServer server) {
			boolean preconditions = (server.getServerType() != null && adaptToViewItem(server) != null
					&& server.getServerState() == IServer.STATE_STARTED);
			return preconditions;
		}

		@Override
		public void selectionChanged(IStructuredSelection sel) {
			if (sel.size() != 1) {
				setEnabled(false);
				return;
			}
			setEnabled(true);
			synchronized (this) {
				switchListener(previousSelection, sel);
				previousSelection = sel;
			}
			super.selectionChanged(sel);
		}

		private void switchListener(IStructuredSelection previousSelection, IStructuredSelection newSel) {
			if (previousSelection != null) {
				Object o = previousSelection.getFirstElement();
				if (o instanceof IServer) {
					((IServer) o).removeServerListener(serverListener);
				}
			}
			Object newSel1 = newSel.getFirstElement();
			if (newSel1 instanceof IServer) {
				((IServer) newSel1).addServerListener(serverListener);
			}
		}

		@Override
		public void perform(final IServer server) {
			// Only run in UI thread

			IWorkbenchPart part = null;
			try {
				part = bringViewToFront(viewId);
			} catch (PartInitException pie) {
				CDKCoreActivator.pluginLog().logError("Error opening view " + viewId, pie);
			}

			if (part != null) {
				final CommonNavigator view = (CommonNavigator) part.getAdapter(CommonNavigator.class);
				if (view != null && view.getCommonViewer() != null && view.getCommonViewer().getTree() != null
						&& !view.getCommonViewer().getTree().isDisposed()) {
					Object connection = adaptToViewItem(server);
					if (connection != null) {
						view.getCommonViewer().expandToLevel(connection, 1);
						ISelection sel = new StructuredSelection(new Object[] { connection });
						view.getCommonViewer().setSelection(sel, true);
					}
				}
			}
		}
	}

	private static final IWorkbenchPart bringViewToFront(String viewId) throws PartInitException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPart part = null;
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				part = page.findView(viewId);
				if (part == null) {
					part = page.showView(viewId);
				} else {
					page.activate(part);
					part.setFocus();
				}
			}
		}
		return part;
	}
}