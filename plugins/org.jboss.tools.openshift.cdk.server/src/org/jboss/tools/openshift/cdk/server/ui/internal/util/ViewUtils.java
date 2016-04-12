/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ViewUtils {
	public static final String TERMINAL_VIEW_ID = "org.eclipse.tm.terminal.view.ui.TerminalsView";

	
	/**
	 * opens the properties view if not already open
	 */
	public static IViewPart openTerminalView() {
		final IViewPart[] ret = new IViewPart[1];
		ret[0] = null;
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				ret[0] = openTerminalViewUI();
			}
		});
		return ret[0];
	}
	
	public static IViewPart openTerminalViewUI() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow activeWindow = wb.getActiveWorkbenchWindow();
			if (activeWindow != null) {
				IWorkbenchPage activePage = activeWindow.getActivePage();
				if (activePage != null) {
					try { 
						return activePage.showView(TERMINAL_VIEW_ID);
					} catch (CoreException ex) {
						// TODO
					}
				}
			}
		}
		return null;
	}
	
}
