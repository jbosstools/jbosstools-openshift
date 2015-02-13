/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.core;

import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

/**
 * 
 * Since the method of prompting a user for credentials is 
 * most likely done via a UI construct, no direct
 * references to the UI may be done in a core plugin. 
 * 
 * Instead, the UI plugin should set a proper handler
 * in the {@link ExpressCoreUIIntegration} class. 
 * 
 * @author Andre Dietisheim
 */
public interface ICredentialsPrompter {

	/**
	 * Prompt that the given connection requires 
	 * authentication. 
	 * 
	 * @param connection  The connection
	 * @return true if prompted for password; false otherwise
	 */
	public boolean promptAndAuthenticate(ExpressConnection connection);
	
}
