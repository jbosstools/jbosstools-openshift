/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
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
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.impl.combo.LabeledCombo;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Server wizard page for CRC.
 * @author odockal
 *
 */
public class NewCRCServerWizardPage extends NewCDK3ServerWizardPage {

	public NewCRCServerWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}
	
	public void setCRCBinary(final String binary) {
		getCRCBinary().setText(binary);
	}
	
	public LabeledText getCRCPullSecretFile() {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		return new LabeledText(CDKLabel.Labels.CRC_PULL_SECRET_FILE);
	}
	
	public void setCRCPullServerFile(final String file) {
		getCRCPullSecretFile().setText(file);
	}
	
	public LabeledText getCRCBinary() {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		return new LabeledText(CDKLabel.Labels.CRC_BINARY);
	}
	
	@Override
	public void setCredentials(String username, String password) {
		throw new CoreLayerException("Setting credentials is not implemented in CRC server wizard page");
	}
	
	@Override
	public LabeledText getMinishiftBinaryLabeledText() {
		throw new CoreLayerException("Minishift Binary label is not implemented in CRC server wizard page, try CRC binary");
	}
	
	@Override
	public LabeledCombo getHypervisorCombo() {
		throw new CoreLayerException("Not yet implemented");
	}
	
}
