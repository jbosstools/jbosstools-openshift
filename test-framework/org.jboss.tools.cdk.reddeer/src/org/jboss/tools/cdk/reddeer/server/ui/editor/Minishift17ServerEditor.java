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

import org.eclipse.reddeer.uiforms.impl.section.DefaultSection;

/**
 * Class representing Minishift 1.7+ server editor
 * @author odockal
 *
 */
public class Minishift17ServerEditor extends MinishiftServerEditor implements CDKPart {

	public Minishift17ServerEditor(String title) {
		super(title);
	}

	@Override
	public DefaultSection getCDKDefaultSection() {
		return getCDKSection();
	}
	
}
