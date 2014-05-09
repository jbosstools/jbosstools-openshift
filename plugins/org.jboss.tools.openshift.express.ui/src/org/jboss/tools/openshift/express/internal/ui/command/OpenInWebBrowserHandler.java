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
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class OpenInWebBrowserHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IApplication application = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IApplication.class);
		if (application == null) {
			return OpenShiftUIActivator.createCancelStatus("Could not find application to show in a browser.");
		}
		final String appName = application.getName();
		final String appUrl = application.getApplicationUrl();
		new BrowserUtility().checkedCreateInternalBrowser(appUrl, appName,
				OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
		return Status.OK_STATUS;
	}

}
