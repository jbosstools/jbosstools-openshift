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
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Class representing CDK3 Server Editor part
 * @author odockal
 *
 */
public class CDK3ServerEditor extends MinishiftServerEditor implements CredentialsPart, CDKPart {
	
	public CDK3ServerEditor(String title) {
		super(title);
	}

	@Override
	public DefaultSection getCDKDefaultSection() {
		return getCDKSection();
	}

	@Override
	public DefaultSection getCredentialsSection() {
		return new DefaultSection(CREDENTIALS);
	}

	@Override
	public LabeledText getBinaryLabel() {
		return getMinishiftBinaryLabel();
	}
	
}
