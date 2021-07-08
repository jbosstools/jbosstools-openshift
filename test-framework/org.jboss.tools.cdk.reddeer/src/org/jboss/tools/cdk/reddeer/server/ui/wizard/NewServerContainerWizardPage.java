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
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

public class NewServerContainerWizardPage extends WizardPage {
	
	public NewServerContainerWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}
	
	public String getDomain() {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		return new LabeledCombo(CDKLabel.Labels.DOMAIN).getSelection();
	}
	
	public Button getAddButton() {
		return new PushButton(CDKLabel.Buttons.ADD);
	}
	
	@SuppressWarnings("unused")
	private void disposeSecureStoragePassword() {
		try {
			new WaitUntil(new ShellIsAvailable(CDKLabel.Shell.SECURE_STORAGE_DIALOG), TimePeriod.MEDIUM, true);
			new DefaultShell(CDKLabel.Shell.SECURE_STORAGE_DIALOG).close();
			new WaitWhile(new ShellIsAvailable(CDKLabel.Shell.SECURE_STORAGE_DIALOG));
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
			String text = credentialsPage.getDescriptionText();
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
