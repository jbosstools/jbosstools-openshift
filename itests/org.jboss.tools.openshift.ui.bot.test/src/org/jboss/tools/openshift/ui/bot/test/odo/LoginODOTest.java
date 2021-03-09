/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.odo;

import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.execution.annotation.RunIf;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.condition.ODOConnectionExists;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.ui.bot.test.AbstractODOTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionCredentialsExists;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Login test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
public class LoginODOTest extends AbstractODOTest  {
	String server = DatastoreOS3.SERVER;
	String username = DatastoreOS3.USERNAME;
	String password = DatastoreOS3.PASSWORD;
	String token = DatastoreOS3.TOKEN;

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewODOBasicConnection() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();

		explorer.connectToOpenShiftODOBasic(server, username, password);
		try {
			new WaitUntil(new ODOConnectionExists(), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			//try to connect once again
			explorer.connectToOpenShiftODOBasic(server, username, password);
			new WaitUntil(new ODOConnectionExists(), TimePeriod.LONG);
		}
		assertTrue("Connection does not exist in OpenShift Application Explorer view",
				explorer.connectionExists());
	}

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewODOOAuthConnection() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();

		explorer.connectToOpenShiftODOOAuth(server, token);

		new WaitUntil(new ODOConnectionExists(), TimePeriod.LONG);
		assertTrue("Connection does not exist in OpenShift Application Explorer view",
				explorer.connectionExists());
	}
}
