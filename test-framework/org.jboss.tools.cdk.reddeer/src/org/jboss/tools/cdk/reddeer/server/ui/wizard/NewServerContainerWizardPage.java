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
package org.jboss.tools.cdk.reddeer.server.ui.wizard;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;

public class NewServerContainerWizardPage {
	
	private static final String SECURE_STORAGE = "Secure Storage Password";

	protected static final String WIZARD_NAME = "New Server";
	
	private static Logger log = Logger.getLogger(NewServerContainerWizardPage.class);
	
	public String getDomain() {
		new DefaultShell(WIZARD_NAME);
		return new LabeledCombo("Domain: ").getSelection();
	}
	
	public Button getAddButton() {
		return new PushButton("Add...");
	}
	
	@SuppressWarnings("unused")
	private static void disposeSecureStoragePassword() {
		try {
			new WaitUntil(new ShellIsAvailable(SECURE_STORAGE), TimePeriod.MEDIUM, true);
			new DefaultShell(SECURE_STORAGE).close();
			new WaitWhile(new ShellIsAvailable(SECURE_STORAGE));
		} catch (WaitTimeoutExpiredException exc) {
			log.info("WaitTimeoutExpiredException occurred while waiting for Secure Storage Password dialog");
		}
	}
	
	public void setCredentials(String username, String password) {
		log.info("Setting Red Hat Access credentials");
		getAddButton().click();
		CredentialsWizardPage credentialsPage = new CredentialsWizardPage();
		credentialsPage.setUsername(username);
		credentialsPage.setPassword(password);
		Button ok = new OkButton();
		if (ok.isEnabled()) {
			ok.click();
		} else {
			String text = credentialsPage.getDescriptionText().getText();
			if (text.length() > 0) {
				log.info("Adding new credential failed to add new user " + username);
				log.info("There is an error while setting Red Hat Credentials: \r\n" + text);
			}
			if (text.contains("already exists for domain access.redhat.com")) {
				credentialsPage.cancelAddingUser();
				return;
			}
			throw new CoreLayerException("Setting the credentials was not successful, "
					+ "OK button is not enabled, error: \n\r" +
					text);	
		}
		// here might be placed code for disposal of secure storage 
	}

}
