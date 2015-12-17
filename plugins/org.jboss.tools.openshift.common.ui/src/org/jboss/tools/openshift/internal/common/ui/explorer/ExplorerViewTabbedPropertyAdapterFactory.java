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
package org.jboss.tools.openshift.internal.common.ui.explorer;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * An adapter factory to provide a tabbed property view in support of the
 * OpenShift explorer
 * 
 * @author jeff.cantrill
 *
 */
public class ExplorerViewTabbedPropertyAdapterFactory implements IAdapterFactory {
	private static final String VIEW_ID = "org.jboss.tools.openshift.express.ui.explorer.expressConsoleView";
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IPropertySheetPage.class) {
			IWorkbenchPart found = findView(VIEW_ID);
			if (found != null) {
				ITabbedPropertySheetPageContributor contrib = new ITabbedPropertySheetPageContributor() {
					public String getContributorId() {
						return VIEW_ID;
					}
				};
				return new TabbedPropertySheetPage(contrib);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { IPropertySheetPage.class };
	}

	private IWorkbenchPart findView(String id) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.findView(id);
				return part;
			}
		}
		return null;
	}
}
