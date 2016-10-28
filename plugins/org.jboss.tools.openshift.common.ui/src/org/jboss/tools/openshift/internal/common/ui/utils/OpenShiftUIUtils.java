/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.utils;

import java.util.Collection;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;

/**
 * @author André Dietisheim
 */
public class OpenShiftUIUtils {

	private OpenShiftUIUtils() {
		//Contains only static methods
	}
	
	public static final String OPENSHIFT_EXPLORER_VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";
	public static final String DOCKER_EXPLORER_VIEW_ID = "org.eclipse.linuxtools.docker.ui.dockerExplorerView";
	
	public static void showOpenShiftExplorer() {
		showViewAsync(OPENSHIFT_EXPLORER_VIEW_ID);
	}
	
	public static void hideOpenShiftExplorer() {
		hideViewAsync(OPENSHIFT_EXPLORER_VIEW_ID);
	}
	
	public static boolean isOpenShiftExplorerVisible() {
		return isViewVisibleSync(OPENSHIFT_EXPLORER_VIEW_ID);
	}
	
	public static void showViewAsync(String viewId) {
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(() -> {
				showView(viewId);
			});
		} else {
			//In UI thread
			showView(viewId);
		}
	}
	
	public static void showView(String viewId) {
		try {
			IWorkbenchPage page = getActivePage();
			if (page != null) {
				page.showView(viewId);
			}
		} catch (PartInitException e) {
			OpenShiftCommonUIActivator.getDefault().getLogger().logError("Failed to show the view "+viewId, e);
		}
	}

	
	public static boolean isViewVisibleSync(String viewId) {
		boolean[] visible = new boolean[1];
		if (Display.getCurrent() == null) {
			Display.getDefault().syncExec(() -> {
				visible[0] = isViewVisible(viewId);
			});
		} else {
			//In UI thread
			visible[0] = isViewVisible(viewId);
		}
		return visible[0];
	}
	
	public static boolean isViewVisible(String viewId) {
		IWorkbenchPage page = getActivePage();
		if (page != null) {
			IViewPart view = page.findView(viewId);
			return view != null && page.isPartVisible(view);
		}
		return false;
	}
	
	public static void hideViewAsync(String viewId) {
		if (Display.getCurrent() == null) {
			Display.getDefault().asyncExec(() -> {
				hideView(viewId);
			});
		} else {
			//In UI thread
			hideView(viewId);
		}
	}
	
	public static void hideView(String viewId) {
		IWorkbenchPage page = getActivePage();
		if (page != null) {
			IViewPart view = page.findView(viewId);
			if (view != null) {
				page.hideView(view);
			}
		}
	}
	
	private static IWorkbenchPage getActivePage() {
		IWorkbenchWindow aww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (aww != null) {
			return aww.getActivePage();
		}
		return null;
	}

	/**
	 * Returns a connection of the given class that can be used in wizards started from
	 * a view other than OpenShift explorer to spare user from first selecting the connection.
	 *
	 * If there is only one available connection of the given class, it is returned.
	 * Otherwise, a selected connection is looked in OpenShift explorer view.
	 * If a child node of a connection is selected, method tries adapter to the class
	 * then considers the parent node.
	 *
	 * @param klass
	 * @return
	 */
	public static <T extends IConnection> T getExplorerDefaultConnection(Class<T> klass) {
		Collection<T> available = ConnectionsRegistrySingleton.getInstance().getAll(klass);
		if(available.size() == 1) {
			//There is only one connection, we do not need it to be selected to pick it.
			return available.iterator().next();
		}
		return getConnectionForExplorerSelection(klass);
	}

	/**
	 * Returns the OpenShift Explorer view part.
	 * 
	 * @return
	 */
	public static IViewPart getOpenShiftExplorer() {
		IWorkbenchPage activePage = getActivePage();
		if(activePage != null) {
			return activePage.findView(OpenShiftUIUtils.OPENSHIFT_EXPLORER_VIEW_ID);
		}
		return null;
	}

	/**
	 * Returns the selection that exists in the OpenShift explorer.
	 * 
	 * @return
	 */
	public static ISelection getOpenShiftExplorerSelection() {
		IViewPart part = getOpenShiftExplorer();
		if (part == null) {
			return null;
		}
		return part.getSite().getSelectionProvider().getSelection();
	}

	public static boolean hasOpenShiftExplorerSelection() {
		ISelection selection = getOpenShiftExplorerSelection();
		return selection != null && !selection.isEmpty();
	}

	/**
	 * Returns the Docker Explorer view part.
	 * 
	 * @return
	 */
	public static IViewPart getDockerExplorer() {
		IWorkbenchPage activePage = getActivePage();
		if(activePage != null) {
			return activePage.findView(OpenShiftUIUtils.DOCKER_EXPLORER_VIEW_ID);
		}
		return null;
	}

	/**
	 * Returns the selection that exists in the Docker explorer.
	 * 
	 * @return
	 */
	public static ISelection getDockerExplorerSelection() {
		IViewPart part = getDockerExplorer();
		if (part == null) {
			return null;
		}
		return part.getSite().getSelectionProvider().getSelection();
	}

	public static boolean hasDockerExplorerSelection() {
		ISelection selection = getDockerExplorerSelection();
		return selection != null && !selection.isEmpty();
	}

	/**
	 * Returns the connection for the given type and current selection in the OpenShift explorer. 
	 * 
	 * @param klass connection type
	 * @return
	 */
	public static <T extends IConnection> T getConnectionForExplorerSelection(Class<T> klass) {
		ISelection selection = getOpenShiftExplorerSelection();
		if (selection != null
				&& !selection.isEmpty()) {
			T result = UIUtils.getFirstElement(selection, klass);
			IViewPart part = getOpenShiftExplorer();
			if (result == null
					&& selection instanceof IStructuredSelection
					&& part instanceof CommonNavigator) {
				Object selected = ((IStructuredSelection) selection).getFirstElement();
				IContentProvider provider = ((CommonNavigator) part).getCommonViewer().getContentProvider();
				if (provider instanceof ITreeContentProvider) {
					ITreeContentProvider tree = (ITreeContentProvider) provider;
					while (selected != null && result == null) {
						result = UIUtils.adapt(selected, klass);
						selected = tree.getParent(selected);
					}
				}
			}
			return result;
		}
		return null;
	}

	/**
	 * Returns the Property Sheet view part.
	 *
	 * @return
	 */
	public static PropertySheet getPropertySheet() {
		IWorkbenchPage activePage = getActivePage();
		if(activePage != null) {
			return (PropertySheet)activePage.findView("org.eclipse.ui.views.PropertySheet"); //$NON-NLS-1$
		}
		return null;
	}

	/**
	 * Refreshes the current page sheet of Property Sheet view.
	 * @param sh
	 */
	public static void refreshPropertySheetPage(PropertySheet propertySheet) {
		if(propertySheet == null) return;
		IPage page = propertySheet.getCurrentPage();
		if(page instanceof TabbedPropertySheetPage) {
			TabbedPropertySheetPage p = (TabbedPropertySheetPage)page;
			if(p == null || p.getControl() == null || p.getControl().isDisposed()) return;
			p.refresh();
		} else if(page instanceof PropertySheetPage) {
			PropertySheetPage p = (PropertySheetPage)page;
			if(p == null || p.getControl() == null || p.getControl().isDisposed()) return;
			p.refresh();
		}
	}
}
