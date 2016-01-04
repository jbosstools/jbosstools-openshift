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

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
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
}
