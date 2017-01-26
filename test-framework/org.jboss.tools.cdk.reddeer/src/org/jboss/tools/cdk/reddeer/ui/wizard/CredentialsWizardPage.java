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

import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.text.LabeledText;

public class CredentialsWizardPage {
	
	public void setUsername(String user) {
		new LabeledText("Username: ").setText(user);
	}
	
	public void togglePromptForPassword(boolean checked) {
		new CheckBox("Always prompt for password").toggle(checked);
	}
	
	public void setPassword(String password) {
		new LabeledText("Password: ").setText(password);
	}
	
	public void toggleShowPassword(boolean checked) {
		new CheckBox("Show password").toggle(checked);
	}
}