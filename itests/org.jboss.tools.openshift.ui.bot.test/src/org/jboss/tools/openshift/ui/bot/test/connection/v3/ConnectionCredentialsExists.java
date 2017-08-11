/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import org.jboss.reddeer.junit.execution.TestMethodShouldRun;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.junit.runners.model.FrameworkMethod;

/**
 * Run new connection test only if credentials for specific test are available.
 * @author mlabuda@redhat.com
 *
 */
public class ConnectionCredentialsExists implements TestMethodShouldRun {

	@Override
	public boolean shouldRun(FrameworkMethod method) {
		if (method.getName().contains("OAuth")) {
			return DatastoreOS3.TOKEN != null && !DatastoreOS3.TOKEN.isEmpty();
		} else {
			return (DatastoreOS3.USERNAME != null && !DatastoreOS3.USERNAME.isEmpty()) 
					&& (DatastoreOS3.PASSWORD != null && !DatastoreOS3.PASSWORD.isEmpty()); 
		}
	}	
}	
