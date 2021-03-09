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

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.widget.terminal.TerminalContainsText;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ODO Commands test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
public class ConnectionODOCommandsTests {

	private OpenShiftODOConnection connection;

	@Before
	public void setUp() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		connection = explorer.getOpenShiftODOConnection();
	}

	@Test
	public void testListCatalogServices() {
		connection.listCatalogServices();
		new WaitUntil(new TerminalContainsText("Operators"), TimePeriod.LONG);
	}

	@Test
	public void testListCatalogComponents() {
		connection.listCatalogComponents();
		new WaitUntil(new TerminalContainsText("Odo Devfile Components"), TimePeriod.LONG);
	}

	@Test
	public void testOpenConsole() {
		connection.openConsole();
		new WaitUntil(new BrowserContainsText("Console"));
	}

	@Test
	public void testAbout() {
		connection.about();
		new WaitUntil(new TerminalContainsText("Server"), TimePeriod.LONG);
	}
}
