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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIActivator;

/**
 * @author André Dietisheim
 */
public class OpenShiftUIUtils {

	public static final String OPENSHIFT_EXPLORER_VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";
	
	public static void showOpenShiftExplorerView() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.showView(OPENSHIFT_EXPLORER_VIEW_ID);
				} catch (PartInitException e) {
					OpenShiftCommonUIActivator.getDefault().getLogger().logError("Failed to show the OpenShift Explorer view", e);
				}
			}
		});
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
	public static <T extends IConnection> T getDefaultConnection(Class<T> klass) {
		Collection<T> available = ConnectionsRegistrySingleton.getInstance().getAll(klass);
		if(available.size() == 1) {
			//There is only one connection, we do not need it to be selected to pick it.
			return available.iterator().next();
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IViewPart part = window.getActivePage().findView(OpenShiftUIUtils.OPENSHIFT_EXPLORER_VIEW_ID);
		if(part != null) {
			ISelection selection = part.getSite().getSelectionProvider().getSelection();
			if(selection != null && !selection.isEmpty()) {
				T result = UIUtils.getFirstElement(selection, klass);
				if(result == null && selection instanceof IStructuredSelection && part instanceof CommonNavigator) {
					Object selected = ((IStructuredSelection)selection).getFirstElement();
					IContentProvider provider = ((CommonNavigator)part).getCommonViewer().getContentProvider();
					if(provider instanceof ITreeContentProvider) {
						ITreeContentProvider tree = (ITreeContentProvider)provider;
						while(selected != null && result == null) {
							result = UIUtils.adapt(selected, klass);
							selected = tree.getParent(selected);
						}
					}
				}
				return result;
			}
		}
		return null;
	}
}
