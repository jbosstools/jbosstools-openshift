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

import org.jboss.reddeer.swt.impl.combo.LabeledCombo;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.LabeledText;

public class NewCDK3ServerContainerWizardPage extends NewServerContainerWizardPage {

	public void setMinishiftBinary(final String binary) {
		new DefaultShell(WIZARD_NAME);
		new LabeledText("Minishift Binary: ").setText(binary);
	}
	
	public void setHypevisor(final String text) {
		new DefaultShell(WIZARD_NAME);
		new LabeledCombo("Hypervisor:").setSelection(text);
	}
	
}
