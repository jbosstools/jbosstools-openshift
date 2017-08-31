/******************************************************************************* 
 * Copyright (c) 2016-2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui;

import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;

public class CDEServersView extends ServersView2 {
	
	protected boolean cdk3 = false;
	
	public CDEServersView(boolean cdk3) {
		this.cdk3 = cdk3;
	}

	@Override
	public Server getServer(String name) {
		return super.getServer(CDEServer.class, name);
	}
	
	
}
