/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.swt.condition.ShellIsActive;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.condition.LoginPageIsLoaded;

/**
 * Class represents process of retrieving new token in openshift wizards
 * 
 * @author odockal
 *
 */
public class AuthenticationTokenRetrival {

	private static final String PAGETITLE_API_TOKEN = "Your API token is";
	private final String OPENSHIFT_USERNAME;
	private final String OPENSHIFT_PASSWORD;

	public AuthenticationTokenRetrival(String username, String password) {
		OPENSHIFT_USERNAME = username;
		OPENSHIFT_PASSWORD = password;
	}

	/**
	 * Represents action of clicking on retrieve new token link, wait for browser to
	 * appear, log in and obtain new token.
	 * 
	 * @return token string
	 */
	public String retrieveToken() {
		EmulatedLinkStyledText linkText = new EmulatedLinkStyledText(OpenShiftLabel.TextLabels.RETRIEVE_TOKEN);
		linkText.click(linkText.getPositionOfText(OpenShiftLabel.TextLabels.LINK_RETRIEVE) + 3);

		new WaitUntil(new ShellIsActive(new WithTextMatcher("Unnamed")), TimePeriod.MEDIUM, false);
		DefaultShell browser = new DefaultShell();
		InternalBrowser internalBrowser = new InternalBrowser(browser);

		login(internalBrowser);

		new WaitUntil(new LoginPageIsLoaded(() -> internalBrowser.getText().contains(PAGETITLE_API_TOKEN)));

		String token = getTokenFromBrowser(internalBrowser);
		// close browser shell
		new PushButton(OpenShiftLabel.Button.CLOSE).click();

		return token;
	}

	private void login(final InternalBrowser browser) {
		new WaitUntil(new LoginPageIsLoaded(() -> containsLoginForm(browser)), TimePeriod.LONG);
		fillAndSubmitCredentials(browser);
	}

	private void fillAndSubmitCredentials(final InternalBrowser internalBrowser) {
		internalBrowser
				.execute(String.format("document.getElementById(\"inputUsername\").value=\"%s\"", OPENSHIFT_USERNAME));
		internalBrowser
				.execute(String.format("document.getElementById(\"inputPassword\").value=\"%s\"", OPENSHIFT_PASSWORD));
		internalBrowser.execute(
				"document.getElementById(\"inputPassword\").parentElement.parentElement.parentElement.submit()");
	}

	private String getTokenFromBrowser(final InternalBrowser internalBrowser) {
		return (String) internalBrowser.evaluate("return document.getElementsByTagName(\"code\")[0].innerHTML");
	}

	private boolean containsLoginForm(InternalBrowser browser) {
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

}
