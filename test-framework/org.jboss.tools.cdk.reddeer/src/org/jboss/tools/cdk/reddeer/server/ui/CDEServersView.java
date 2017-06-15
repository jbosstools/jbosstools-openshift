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

import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.tools.cdk.reddeer.server.ui.CDEServer;

public class CDEServersView extends ServersView {
	
	protected boolean cdk3 = false;
	
	public CDEServersView(boolean cdk3) {
		this.cdk3 = cdk3;
	}
	
	@Override
	protected Server createServer(TreeItem item) {
		return new CDEServer(item, this, this.cdk3);
	}
}
