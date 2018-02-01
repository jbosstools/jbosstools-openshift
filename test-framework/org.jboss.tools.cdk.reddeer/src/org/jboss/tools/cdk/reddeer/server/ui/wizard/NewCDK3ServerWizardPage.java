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

import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

public class NewCDK3ServerWizardPage extends NewServerContainerWizardPage {

	public LabeledText getMinishiftBinaryLabeledText() {
		new DefaultShell(WIZARD_NAME);
		return new LabeledText("Minishift Binary: ");	
	}
	
	public void setMinishiftBinary(final String binary) {
		getMinishiftBinaryLabeledText().setText(binary);
	}
	
	public LabeledCombo getHypervisorCombo() {
		new DefaultShell(WIZARD_NAME);
		return new LabeledCombo("Hypervisor:");		
	}
	
	public void setHypervisor(final String text) {
		getHypervisorCombo().setSelection(text);
	}
	
}
