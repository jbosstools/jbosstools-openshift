/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.ui.view;

import java.util.Arrays;
import java.util.List;

import org.eclipse.wst.server.core.IServer;

public class CRCActionProvider extends CDKActionProvider {

	protected String getSetupActionName() {
		return "Setup CRC";
	}
	
	protected List<String> getSetupServerTypes() {
		return Arrays.asList(new String[] {
				"org.jboss.tools.openshift.cdk.server.type.crc.v100"
		});
	}
	
	protected void scheduleSetupJob(IServer server) {
		new SetupCRCJob(server, actionSite.getViewSite().getShell()).schedule();
	}
}