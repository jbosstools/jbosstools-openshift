/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.condition.WaitCondition;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.execution.annotation.RunIf;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.Browser;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.common.reddeer.label.IDELabel.ServerType;
import org.jboss.tools.common.reddeer.utils.StackTraceUtils;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftExplorerRequirement.CleanOpenShiftExplorer;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.EmulatedLinkStyledText;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView.AuthenticationMethod;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author mlabuda@redhat.com
 * @author adietish@redhat.com
 */
@SuppressWarnings("unused")
@CleanOpenShiftExplorer
@RunWith(RedDeerSuite.class)
public class CreateNewConnectionTest {

	private static Logger LOG = new Logger(CreateNewConnectionTest.class);
	private static final String PAGETITLE_API_TOKEN = "Your API token is";

	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3BasicConnection() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		DatastoreOS3.AUTH_METHOD = AuthenticationMethod.BASIC;
		
		explorer.openConnectionShell();
		try {
			explorer.connectToOpenShift3Basic(DatastoreOS3.SERVER, DatastoreOS3.USERNAME, 
					DatastoreOS3.PASSWORD, false, false);
		} catch (RedDeerException ex) {
			LOG.error(StackTraceUtils.stackTraceToString(ex));
			fail("Creating an OpenShift v3 basic connection failed.");
		}
		
		assertTrue("Connection does not exist in OpenShift Explorer view", 
				explorer.connectionExists(DatastoreOS3.USERNAME));
	}
	
	@Test
	@RunIf(conditionClass = ConnectionCredentialsExists.class)
	public void testCreateNewV3OAuthConnection() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		DatastoreOS3.AUTH_METHOD = AuthenticationMethod.OAUTH;
		
		explorer.openConnectionShell();
		try {
			explorer.connectToOpenShift(DatastoreOS3.SERVER, null, DatastoreOS3.TOKEN, false, false, OpenShiftExplorerView.AuthenticationMethod.OAUTH, false);
		} catch (RedDeerException ex) {
			fail("Creating an OpenShift v3 basic connection failed." + ex.getCause());
		}
	
		assertTrue("Connection does not exist in OpenShift Explorer view", 
				explorer.connectionExists(DatastoreOS3.USERNAME));
	}
	
	@Test
	public void shouldExtractTokenInBrowserWindow() {
		openConnectionWizardAndSetDefaultServerOAuth();

		EmulatedLinkStyledText linkText = new EmulatedLinkStyledText(OpenShiftLabel.TextLabels.RETRIEVE_TOKEN);
		linkText.click(linkText.getPositionOfText(OpenShiftLabel.TextLabels.LINK_RETRIEVE) + 3);
		
		new DefaultShell("Untrusted SSL Certificate");
		new YesButton().click();
		
		DefaultShell browser = new DefaultShell();
		InternalBrowser internalBrowser = new InternalBrowser(browser);

		login(internalBrowser);

		new WaitUntil(new LoginPageIsLoaded(() -> internalBrowser.getText().contains(PAGETITLE_API_TOKEN)));

		String token = getTokenFromBrowser(internalBrowser);
		// close browser shell
		new PushButton(OpenShiftLabel.Button.CLOSE).click();

		String tokenText = new LabeledText(OpenShiftLabel.TextLabels.TOKEN).getText();

		assertEquals(token, tokenText);

		new CancelButton().click();
	}

	private void openConnectionWizardAndSetDefaultServerOAuth() {
		OpenShiftExplorerView openShiftExplorerView = new OpenShiftExplorerView();
		openShiftExplorerView.open();
		openShiftExplorerView.openConnectionShell();
		new DefaultShell(OpenShiftLabel.Shell.NEW_CONNECTION);
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVER_TYPE)
				.setSelection(OpenShiftLabel.Others.OPENSHIFT3);
		new LabeledCombo(OpenShiftLabel.TextLabels.SERVER).setText(DatastoreOS3.SERVER);
		new LabeledCombo(OpenShiftLabel.TextLabels.PROTOCOL).setSelection(AuthenticationMethod.OAUTH.toString());
	}

	private void login(final InternalBrowser internalBrowser) {
		new WaitUntil(new LoginPageIsLoaded(() -> containsLoginForm(internalBrowser)),TimePeriod.LONG);
		fillAndSubmitCredentials(internalBrowser);
	}

	private boolean containsLoginForm(Browser browser) {
		try {
			Object evaluate = browser.evaluate("return document.getElementsByTagName(\"form\")[0].innerHTML;");
			if (!(evaluate instanceof String)) {
				return false;
			}
			return ((String) evaluate).contains("name=\"username\"");
		} catch (CoreLayerException e) {
			return false;
		}
	}

	private void fillAndSubmitCredentials(final InternalBrowser internalBrowser) {
		internalBrowser.execute(
				String.format("document.getElementById(\"inputUsername\").value=\"%s\"", DatastoreOS3.USERNAME));
		internalBrowser.execute(
				String.format("document.getElementById(\"inputPassword\").value=\"%s\"", DatastoreOS3.PASSWORD));
		internalBrowser.execute(
				"document.getElementById(\"inputPassword\").parentElement.parentElement.parentElement.submit()");
	}

	private String getTokenFromBrowser(final InternalBrowser internalBrowser) {
		return (String) internalBrowser.evaluate("return document.getElementsByTagName(\"code\")[0].innerHTML");
	}

	private class LoginPageIsLoaded implements WaitCondition {

		private TestCondition myTest;

		public LoginPageIsLoaded(TestCondition myTest) {
			this.myTest = myTest;
		}

		@Override
		public boolean test() {
			return myTest.test();
		}

		@Override
		public String description() {
			return "Login page is loaded";
		}

		@Override
		public <T> T getResult() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String errorMessageWhile() {
			return "Login page is not fully loaded";
		}

		@Override
		public String errorMessageUntil() {
			return "Login page is not fully loaded";
		}

	}

	private interface TestCondition {
		public boolean test();
	}

}
