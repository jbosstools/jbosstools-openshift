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

import org.eclipse.reddeer.swt.impl.text.LabeledText;

/**
 * CKD 3.2+ server editor class
 * @author odockal
 *
 */
public class CDK32ServerEditor extends CDK3ServerEditor {

	public CDK32ServerEditor(String title) {
		super(title);
	}

	public LabeledText getMinishiftProfile() {
		return new LabeledText(getCDKSection(), "Minishift Profile:");
	}
	
}
