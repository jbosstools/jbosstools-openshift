/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.ui;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.io.core.LoginProvider;
import org.jboss.tools.openshift.io.core.LoginResponse;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.jboss.tools.openshift.io.internal.ui.dialog.BrowserBasedLoginDialog;

/**
 * Login provider that will launch a browser to perform the login and extract the JSON.
 * 
 */
public class DefaultLoginProvider implements LoginProvider {


	@Override
	public LoginResponse login(ICluster cluster, IAccount account) {
		LoginResponse response = null;
		
		BrowserBasedLoginDialog dialog = new BrowserBasedLoginDialog(Display.getCurrent().getActiveShell(), cluster.getLoginURL(), cluster.getLandingURL());
		if (dialog.open() == Window.OK) {
			response = dialog.getInfo();
		}
		return response;
	}
}
