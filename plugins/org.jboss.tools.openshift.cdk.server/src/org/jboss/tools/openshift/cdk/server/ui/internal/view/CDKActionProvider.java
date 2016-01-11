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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.internal.view.servers.AbstractServerAction;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.ControllableServerBehavior;
import org.jboss.ide.eclipse.as.wtp.ui.WTPOveridePlugin;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.ADBInfo;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKDockerUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.listeners.CDKOpenshiftUtility;

public class CDKActionProvider extends CommonActionProvider {
	private ICommonActionExtensionSite actionSite;
	private ShowInViewAfterStartupAction showInOpenshiftViewAction;
	private ShowInViewAfterStartupAction showInDockerViewAction;
	
	private static final String DOCKER_VIEW_ID = "org.eclipse.linuxtools.docker.ui.dockerExplorerView";
	private static final String OPENSHIFT_VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";
	
	public CDKActionProvider() {
		super();
	}
	
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		this.actionSite = aSite;
		createActions(aSite);
	}

	protected void createActions(ICommonActionExtensionSite aSite) {
		ICommonViewerSite site = aSite.getViewSite();
		if( site instanceof ICommonViewerWorkbenchSite ) {
			StructuredViewer v = aSite.getStructuredViewer();
			if( v instanceof CommonViewer ) {
				CommonViewer cv = (CommonViewer)v;
				ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite)site;
				showInOpenshiftViewAction = new ShowInOpenshiftViewAfterStartupAction(wsSite.getSelectionProvider(), OPENSHIFT_VIEW_ID);
				showInDockerViewAction = new ShowInDockerViewAfterStartupAction(wsSite.getSelectionProvider(), DOCKER_VIEW_ID);
			}
		}
	}

	private static class ShowInOpenshiftViewAfterStartupAction extends ShowInViewAfterStartupAction {
		public ShowInOpenshiftViewAfterStartupAction(ISelectionProvider sp, String viewId) {
			super(sp, viewId);
		}

		protected Object adaptToViewItem(IServer server) {
			ControllableServerBehavior beh = (ControllableServerBehavior)server.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
			ADBInfo adb = null;
			if( beh != null ) {
				adb = (ADBInfo)beh.getSharedData(ADBInfo.SHARED_INFO_KEY);
			}
			if( adb != null )
				return new CDKOpenshiftUtility().findExistingOpenshiftConnection(server, adb);
			return null;
		}
	}

	private static class ShowInDockerViewAfterStartupAction extends ShowInViewAfterStartupAction {
		public ShowInDockerViewAfterStartupAction(ISelectionProvider sp, String viewId) {
			super(sp, viewId);
		}

		protected Object adaptToViewItem(IServer server) {
			ControllableServerBehavior beh = (ControllableServerBehavior)server.loadAdapter(ControllableServerBehavior.class, new NullProgressMonitor());
			ADBInfo adb = null;
			if( beh != null ) {
				adb = (ADBInfo)beh.getSharedData(ADBInfo.SHARED_INFO_KEY);
			}
			if( adb != null )
				return new CDKDockerUtility().findDockerConnection(adb);
			return null;
		}

	}

	
	public void fillContextMenu(IMenuManager menu) {
		ICommonViewerSite site = actionSite.getViewSite();
		IStructuredSelection selection = null;
		if (site instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite wsSite = (ICommonViewerWorkbenchSite) site;
			selection = (IStructuredSelection) wsSite.getSelectionProvider()
					.getSelection();
		}

		IContributionItem quick = menu.find("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
		if( quick != null && selection != null && selection.size() == 1 ) {
			if( selection.getFirstElement() instanceof IServer ) {
				IServer server = (IServer)selection.getFirstElement();
				if( acceptsServer(server) ) {
					if( menu instanceof MenuManager ) {
						((MenuManager)quick).add(showInDockerViewAction);
						((MenuManager)quick).add(showInOpenshiftViewAction);
					}
				}
			}
		}
	}
	
	private boolean acceptsServer(IServer s) {
		// For now lets just do cdk servers, but we can change this if we wanted it extensible via an adapter?
		if( s != null && s.getServerType() != null && s.getServerType().getId().equals(CDKServer.CDK_SERVER_TYPE))
			return true;
		return false;
	}
	
	private static class ShowInViewAfterStartupAction extends AbstractServerAction { 
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
				public void serverChanged(final ServerEvent event) {
					// If this is the server that was / is selected
					if( previousSelection != null && previousSelection.size() > 0 
							&& previousSelection.getFirstElement().equals(event.getServer())) {
						// and it switches state, update enablement
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								if( UnitedServerListener.serverSwitchesToState(event, IServer.STATE_STARTED)) {
									setEnabled(true);
								} else if( UnitedServerListener.serverSwitchesToState(event, IServer.STATE_STOPPED)) {
									setEnabled(false);
								}
							}
						});
					}
				}
			};
			selectionChanged(sp.getSelection());
		}
		
		protected Object adaptToViewItem(IServer server) {
			// Subclass override this
			return null;
		}
		
		public boolean accept(IServer server) {
			boolean preconditions = (server.getServerType() != null && 
					adaptToViewItem(server) != null
					&& server.getServerState() == IServer.STATE_STARTED);
			return preconditions;
		}
		
		public void selectionChanged(IStructuredSelection sel) {
			if( sel.size() != 1 ) {
				setEnabled(false);
				return;
			}
			setEnabled(true);
			synchronized(this) {
				switchListener(previousSelection, sel);
				previousSelection = sel;
			}
			super.selectionChanged(sel);
		}

		private void switchListener(IStructuredSelection previousSelection, IStructuredSelection newSel) {
			if( previousSelection != null ) {
				Object o = previousSelection.getFirstElement();
				if( o instanceof IServer) {
					((IServer)o).removeServerListener(serverListener);
				}
			}
			Object newSel1 = newSel.getFirstElement();
			if( newSel1 instanceof IServer) {
				((IServer)newSel1).addServerListener(serverListener);
			}
		}
		
		public void perform(final IServer server) {
			// Only run in UI thread
			
			IWorkbenchPart part = null;
			try {
				part = bringViewToFront(viewId);
			} catch(PartInitException pie) {
				CDKCoreActivator.pluginLog().logError("Error opening view " + viewId, pie);
			}
			
			if( part != null ) {
				final CommonNavigator view = (CommonNavigator) part.getAdapter(CommonNavigator.class);
				if (view != null && view.getCommonViewer() != null && view.getCommonViewer().getTree() != null && !view.getCommonViewer().getTree().isDisposed()){
					Object connection = adaptToViewItem(server);
					if( connection != null ) {
						view.getCommonViewer().collapseAll();
						view.getCommonViewer().expandToLevel(2);
						ISelection sel = new StructuredSelection(new Object[] { connection });
						view.getCommonViewer().setSelection(sel, true);
					}
				}
			}
		}
	}
	private static final IWorkbenchPart bringViewToFront(String viewId) throws PartInitException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow() ;
		IWorkbenchPart part = null;
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				part = page.findView(viewId);
				if (part == null) {
					part = page.showView(viewId);
				}
				if (part != null) {
					page.activate(part);
					part.setFocus();
				}
			}
		}
		return part;
	}
}