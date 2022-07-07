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

import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.browser.InternalBrowserRequirement.UseInternalBrowser;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
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
@UseInternalBrowser
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
		connection.openConsole(); // prepare requirement for using internal browser

		closeSSLDialog("");
		closeSSLDialog("");
		new WaitUntil(new BrowserContainsText("oauth"));
	}

	@Test
	public void testAbout() {
		connection.about();
		new WaitUntil(new TerminalContainsText("Server"), TimePeriod.LONG);
	}
	
	private void closeSSLDialog(String text) {
		ShellIsAvailable sslDialog = new ShellIsAvailable(text);
		new WaitUntil(sslDialog, TimePeriod.MEDIUM, false);
		try {
			Display.syncExec(new Runnable() {

				@Override
				public void run() {
					DefaultShell ssl = new DefaultShell();
					if (ssl.getText().toLowerCase().contains("certificate")) {
						new PushButton("Yes").click();
					}
				}
			});

		} catch (CoreLayerException exc) {
			// no dialog
		}
		if(sslDialog.getResult() != null) {
			new DefaultShell(sslDialog.getResult());
			new PushButton("Yes").click();
		}
	}
}
