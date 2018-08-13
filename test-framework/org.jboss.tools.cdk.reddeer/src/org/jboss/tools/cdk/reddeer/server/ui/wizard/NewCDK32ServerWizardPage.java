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

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * New CDK 3.2+ server container wizard page
 * @author odockal
 *
 */
public class NewCDK32ServerWizardPage extends NewCDK3ServerWizardPage {

	public NewCDK32ServerWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void setMinishiftProfile(final String profile) {
		getMinishiftProfile().setText(profile);
	}
	
	public LabeledText getMinishiftProfile() {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		return new LabeledText(CDKLabel.Labels.MINISHIFT_PROFILE);
	}
	
	public void setMinishiftHome(final String home) {
		getMinishiftHome().setText(home);
	}
	
	public LabeledText getMinishiftHome() {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		return new LabeledText(CDKLabel.Labels.MINISHIFT_HOME);
	}

}
