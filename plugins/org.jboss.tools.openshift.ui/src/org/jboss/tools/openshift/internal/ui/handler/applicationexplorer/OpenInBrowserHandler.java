/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.URLElement;

/**
 * @author Red Hat Developers
 */
public class OpenInBrowserHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		URLElement urlElement = UIUtils.getFirstElement(selection, URLElement.class);
		if (urlElement == null) {
			return OpenShiftUIActivator.statusFactory().cancelStatus("No url selected"); //$NON-NLS-1$
		}
		String url = urlElement.getWrapped().getProtocol() + "://" + urlElement.getWrapped().getHost();
		new BrowserUtility().checkedCreateInternalBrowser(url, url, OpenShiftUIActivator.PLUGIN_ID,
		        OpenShiftUIActivator.getDefault().getLog());
		return null;
	}

}
