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
package org.jboss.tools.cdk.reddeer.ui.wizard;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.swt.api.Button;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;

public class NewServerContainerWizardPage {

	protected static final String WIZARD_NAME = "New Server";
	
	private static Logger log = Logger.getLogger(NewServerContainerWizardPage.class);
	
	public String getDomain() {
		return new LabeledText("Domain: ").getText();
	}
	
	public Button getAddButton() {
		return new PushButton("Add...");
	}
	
	private static void disposeSecureStoragePassword() {
		try {
			new WaitUntil(new ShellWithTextIsAvailable("Secure Storage Password"), TimePeriod.NORMAL, true);
			new DefaultShell("Secure Storage Password").close();
			new WaitWhile(new ShellWithTextIsAvailable("Secure Storage Password"));
		} catch (WaitTimeoutExpiredException exc) {
			log.info("WaitTimeoutExpiredException occurred while waiting for Secure Storage Password dialog");
		}
	}
	
	public void setCredentials(String username, String password) {
		getAddButton().click();
		CredentialsWizardPage credentialsPage = new CredentialsWizardPage();
		credentialsPage.setUsername(username);
		credentialsPage.setPassword(password);
		Button ok = new OkButton();
		if (ok.isEnabled()) {
			ok.click();
		} else {
			throw new CoreLayerException("Setting credentials was not successful, "
					+ "OK button is not enabled");
		}
		disposeSecureStoragePassword();
	}

}
