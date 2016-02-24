/*******************************************************************************
* Copyright (c) 2015 Red Hat, Inc.
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IResource;

/**
 * Handler for displaying {@link IResource} properties.
 * 
 * @author Fred Bricon
 */
public class ShowPropertiesHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage();
					IWorkbenchPart active = page.getActivePart();
					IWorkbenchPart part = page.showView("org.eclipse.ui.views.PropertySheet");
					page.activate(active);
					page.activate(part);
				} catch (PartInitException e) {
					OpenShiftUIActivator.getDefault().getLogger().logError("Failed to show the Properties view", e);
				}
			}
		});
		return null;
	}


}