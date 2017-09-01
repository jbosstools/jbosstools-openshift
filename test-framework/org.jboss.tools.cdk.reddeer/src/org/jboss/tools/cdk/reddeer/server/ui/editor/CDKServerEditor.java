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
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.jboss.reddeer.swt.api.Button;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.text.LabeledText;
import org.jboss.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Class representing CDK 2.x Server Editor page
 * @author odockal
 *
 */
public class CDKServerEditor extends CDEServerEditor {

	public CDKServerEditor(String title) {
		super(title);
	}

	public LabeledText getVagrantfileLocation() {
		return new LabeledText(new DefaultSection(CDK_DETAILS), "Vagrantfile Location: ");
	}

	public Button getVagrantfileBrowse() {
		return new PushButton(new DefaultSection(CDK_DETAILS), "Browse...");
	}

}
