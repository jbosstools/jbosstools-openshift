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

import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * Parenting server wizard class for CDK/Minishift 
 * @author odockal
 *
 */
public class NewCDKServerWizardPage extends NewServerContainerWizardPage {

	public void setFolder(final String folder) {
		new DefaultShell(WIZARD_NAME);
		new LabeledText("Folder: ").setText(folder);
	}
	
}
