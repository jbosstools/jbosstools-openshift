/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.junit.execution.TestMethodShouldRun;
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
			return StringUtils.isNotEmpty(DatastoreOS3.TOKEN);
		} else {
			return (StringUtils.isNotEmpty(DatastoreOS3.USERNAME) && StringUtils.isNotEmpty(DatastoreOS3.SERVER)); 
		}
	}	
}	
