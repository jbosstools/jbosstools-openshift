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
package org.jboss.tools.cdk.reddeer.server.ui.editor;

import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;

/**
 * Represents CRC server editor page object.
 * @author odockal
 *
 */
public class CRCServerEditor extends MinishiftServerEditor implements CRCPart {
	
	public CRCServerEditor(String title) {
		super(title);
	}
	
	@Override
	public DefaultSection getDefaultSection() {
		return new DefaultSection(CDKLabel.Sections.CRC_DETAILS);
	}

	@Override
	public DefaultSection getCDKDefaultSection() {
		return getCDKSection();
	}

	@Override
	public LabeledText getBinaryLabel() {
		return getCRCBinary();
	}

}
