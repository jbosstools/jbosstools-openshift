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
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.browser.InternalBrowserRequirement.UseInternalBrowser;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement.RequiredODOProject;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.widget.terminal.TerminalContainsText;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
@RequiredODOProject(name = "test-project-operator", cleanup = true)
public class ConnectionODOCommandsTests {

	private OpenShiftODOConnection connection;

	@After
	public void cleanUp() {
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
		new WaitWhile(new JobIsRunning(new WithTextMatcher("Refresh cluster")), TimePeriod.DEFAULT, false);
	}
	
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

	/**
	 * This test case is not working as Opening a console for OS explorer app. connection
	 * opens up a site with self-signed certificate which is evaluated as invalid, giving an user
	 * option to accept it, using native message box, which cannot be handled by reddeer.
	 */
	@Ignore
	@Test
	public void testOpenConsole() {
		connection.openConsole(); // prepare requirement for using internal browse
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
