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
package org.jboss.tools.cdk.reddeer.server.ui.wizard;

import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Text;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Credentials wizard page for new server
 * @author odockal
 *
 */
public class CredentialsWizardPage {
	
	public void activate() {
		new DefaultShell(CDKLabel.Shell.ADD_CREDENTIALS_DIALOG);
	}
	
	public Text getDescriptionText() {
		return new LabeledText(CDKLabel.Labels.ADD_CREDENTIAL);
	}
	
	public void setUsername(String user) {
		new LabeledText(CDKLabel.Labels.USERNAME).setText(user);
	}
	
	public void togglePromptForPassword(boolean checked) {
		new CheckBox(CDKLabel.Buttons.ALWAYS_PROMPT_FOR_PASSWORD).toggle(checked);
	}
	
	public void setPassword(String password) {
		new LabeledText(CDKLabel.Labels.PASSWORD).setText(password);
	}
	
	public void toggleShowPassword(boolean checked) {
		new CheckBox(CDKLabel.Buttons.SHOW_PASSWORD).toggle(checked);
	}
	
	public void cancelAddingUser() {
		try {
			Button cancel = new CancelButton();
			cancel.click();
		} catch (CoreLayerException exc) {
			throw new CoreLayerException("Canceling setting the credentials was not successful");	
		}
	}
}