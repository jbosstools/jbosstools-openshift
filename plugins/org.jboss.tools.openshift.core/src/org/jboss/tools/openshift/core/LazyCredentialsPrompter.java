/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.IConnection;

/**
 * A class to support deferred initialization where the actual provider may
 * be initialized after the class that depends upon this on
 */
public class LazyCredentialsPrompter implements ICredentialsPrompter {
	
	private ICredentialsPrompter prompter;
	
	/**
	 * Prompt for authentication.
	 */
	@Override
	public final boolean promptAndAuthenticate(IConnection connection, Object context) {
		if (!loadPrompter()) {
			return false;
		}
		return prompter.promptAndAuthenticate(connection, context);
	}

	private boolean loadPrompter() {
		if (prompter == null) {
			prompter = getExtension();
			return prompter != null;
		}
		return true;
	}

	// for testing purposes
	public ICredentialsPrompter getExtension() {
		return OpenShiftCoreUIIntegration.getInstance().getCredentialPrompter();
	}
	
}
