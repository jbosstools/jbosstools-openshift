/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core;

import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.core.ICredentialsPrompter;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;

/**
 * A class to support deferred initialization where the actual provider may
 * be initialized after the class that depends upon this on
 */
public final class LazyCredentialsPrompter implements ICredentialsPrompter {
	
	private ICredentialsPrompter prompter;
	
	public LazyCredentialsPrompter(ICredentialsPrompter prompter) {
		if(prompter instanceof LazyCredentialsPrompter){
			throw new IllegalArgumentException("Unable to initialize a LazyCredentialsPrompter with instance of the same type");
		}
		this.prompter = prompter;
	}
	
	/**
	 * Prompt for authentication.
	 */
	@Override
	public final boolean promptAndAuthenticate(ExpressConnection connection) {
		if(prompter == null){
			prompter = ExpressCoreUIIntegration.getDefault().getCredentialPrompter();
			if(prompter == null) return false;
		}
		return prompter.promptAndAuthenticate(connection);
	}

}
