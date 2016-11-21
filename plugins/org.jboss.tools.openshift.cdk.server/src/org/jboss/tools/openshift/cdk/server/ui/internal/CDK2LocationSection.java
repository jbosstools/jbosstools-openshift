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
package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;

import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDK2LocationSection extends AbstractLocationSection {

	private static String SECTION_TITLE = "CDK Details";
	private static String LABEL_STRING = "Vagrantfile Location: ";
	private static String COMMAND_NAME = "Modify Vagrantfile Location";
	private static String LOC_ATTR = CDKServer.PROP_FOLDER;
	
	public CDK2LocationSection() {
		super(SECTION_TITLE, LABEL_STRING, COMMAND_NAME, LOC_ATTR);
	}

	@Override
	protected File getFile(File selected, Shell shell) {
		return chooseDirectory(selected, shell);
	}
	
}
