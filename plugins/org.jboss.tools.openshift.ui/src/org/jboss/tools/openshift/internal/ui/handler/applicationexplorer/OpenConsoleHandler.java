/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;

import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 */
public class OpenConsoleHandler extends OdoHandler {

	@Override
	public void actionPerformed(Odo odo) throws IOException {
		String url = odo.consoleURL();
		new BrowserUtility().checkedCreateInternalBrowser(url, url, OpenShiftUIActivator.PLUGIN_ID,
				OpenShiftUIActivator.getDefault().getLog());
	}

}
