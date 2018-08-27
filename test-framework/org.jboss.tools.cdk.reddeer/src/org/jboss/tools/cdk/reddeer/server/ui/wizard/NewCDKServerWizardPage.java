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

import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Parenting server wizard class for CDK/Minishift 
 * @author odockal
 *
 */
public class NewCDKServerWizardPage extends NewServerContainerWizardPage {

	public NewCDKServerWizardPage(ReferencedComposite referencedComposite) {
		super(referencedComposite);
	}

	public void setFolder(final String folder) {
		new DefaultShell(CDKLabel.Shell.NEW_SERVER_WIZARD);
		new LabeledText(CDKLabel.Labels.FOLDER).setText(folder);
	}
	
}
