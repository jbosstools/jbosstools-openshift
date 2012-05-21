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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.jface.viewers.ITreeSelection;
import org.jboss.tools.common.ui.BrowserUtil;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.IApplication;

/**
 * @author Xavier Coulon
 */
public class OpenInWebBrowserAction extends AbstractAction {

	public OpenInWebBrowserAction() {
		super(OpenShiftExpressUIMessages.SHOW_IN_BROWSER_ACTION, true);
		setImageDescriptor(OpenShiftImages.OPEN_BROWSER);
	}

	/**
	 * Operation called when the user clicks on 'Show In>Remote Console'. If no Console/Worker existed, a new one is
	 * created, otherwise, it is displayed. {@inheritDoc}
	 */
	@Override
	public void run() {
		if (selection != null && selection instanceof ITreeSelection
				&& ((ITreeSelection) selection).getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) ((ITreeSelection) selection).getFirstElement();
			final String appName = application.getName();
			final String appUrl = application.getApplicationUrl();
			BrowserUtil.checkedCreateInternalBrowser(appUrl, appName,
					OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
		}
	}

}
