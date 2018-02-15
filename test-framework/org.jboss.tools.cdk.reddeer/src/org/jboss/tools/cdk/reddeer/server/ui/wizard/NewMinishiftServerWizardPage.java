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
package org.jboss.tools.cdk.reddeer.server.ui.wizard;

import org.eclipse.reddeer.common.logging.Logger;

/**
 * New minishift server wizard page class rep.
 * @author odockal
 *
 */
public class NewMinishiftServerWizardPage extends NewCDK32ServerWizardPage {

	private static final Logger log = Logger.getLogger(NewMinishiftServerWizardPage.class);
	
	@Override
	public void setCredentials(String username, String password) {
		log.info("Setting credentials in New Minishift Server wizard page is no possible");
	}
}
