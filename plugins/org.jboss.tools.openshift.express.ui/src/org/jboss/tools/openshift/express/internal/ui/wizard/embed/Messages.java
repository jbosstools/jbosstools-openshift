/******************************************************************************* 

 * Copyright (c) 2014 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS{
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.express.internal.ui.wizard.embed.messages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String DESELECT_ALL_CARTRIDGES_TITLE;
	public static String DESELECT_ALL_CARTRIDGES_DESCRIPTION;
}