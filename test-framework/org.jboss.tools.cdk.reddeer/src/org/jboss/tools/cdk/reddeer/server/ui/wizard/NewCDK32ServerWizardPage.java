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

import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * New CDK 3.2+ server container wizard page
 * @author odockal
 *
 */
public class NewCDK32ServerWizardPage extends NewCDK3ServerWizardPage {

	public void setMinishiftProfile(final String profile) {
		getMinishiftProfile().setText(profile);
	}
	
	public LabeledText getMinishiftProfile() {
		new DefaultShell(WIZARD_NAME);
		return new LabeledText("Minishift Profile:");
	}

}
