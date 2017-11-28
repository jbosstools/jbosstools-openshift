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
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;

public class CDKServersView extends ServersView2 {

	public CDKServersView() {
		super();
	}
	
	@Override
	public Server getServer(String name) {
		return getServer(CDKServer.class, name);
	}

	/**
	 * Calls parent's newServer method, executes its steps
	 * and return new CDK server wizard instead
	 * 
	 * @return Wizard for adding new cdk server
	 */
	public NewCDKServerWizard newCDKServer() {
		super.newServer();
		return new NewCDKServerWizard();
	}		
}
