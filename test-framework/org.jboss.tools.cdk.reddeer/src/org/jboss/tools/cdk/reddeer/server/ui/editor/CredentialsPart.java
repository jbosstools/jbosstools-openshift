/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.eclipse.reddeer.swt.api.Button;
import org.eclipse.reddeer.swt.api.Combo;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Interface with default implementation of Credentials part of CDK server editor
 * @author odockal
 *
 */
public interface CredentialsPart {
	
	public DefaultSection getCredentialsSection();

	public default LabeledText getPasswordLabel() {
		return new LabeledText(getCredentialsSection(), "Password: ");
	}
	
	public default LabeledText getUsernameLabel() {
		return new LabeledText(getCredentialsSection(), "Username: ");
	}
	
	public default Button getAddButton() {
		return new PushButton(getCredentialsSection(), "Add...");
	}
	
	public default Button getEditButton() {
		return new PushButton(getCredentialsSection(), "Edit...");
	}
	
	public default Combo getDomainCombo() {
		return new LabeledCombo(getCredentialsSection(), "Domain: ");
	}
	
	public default Button getPassCredentialsCheckBox() {
		return new CheckBox(getCredentialsSection(), "Pass credentials to environment");
	}
	
}
