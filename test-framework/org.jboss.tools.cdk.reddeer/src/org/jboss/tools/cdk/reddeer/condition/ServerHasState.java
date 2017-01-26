/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.condition;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersViewEnums.ServerState;

/**
 * Class represents wait condition that fulfills test whether server has given state
 * @author odockal
 *
 */
public class ServerHasState extends AbstractWaitCondition {

	private ServerState state;
	
	private Server server;
	
	public ServerHasState(Server server, ServerState state) {
		this.server = server;
		this.state = state;
	}
	
	@Override
	public boolean test() {
		return state.equals(server.getLabel().getState());
	}

	@Override
	public String description() {
		return "server's state is: " + state.getText();
	}
	
}
