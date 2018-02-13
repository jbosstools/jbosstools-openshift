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
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Class representing CDK 2.x Server Editor page
 * @author odockal
 *
 */
public class CDK2ServerEditor extends MinishiftServerEditor implements CredentialsPart {
	
	public CDK2ServerEditor(String title) {
		super(title);
	}

	public LabeledText getVagrantfileLocation() {
		return new LabeledText(getCDKSection(), "Vagrantfile Location: ");
	}
	
	public Button getVagrantfileBrowse() {
		return new PushButton(getCDKSection(), "Browse...");
	}

	@Override
	public DefaultSection getCredentialsSection() {
		return new DefaultSection(CREDENTIALS);
	}

}
