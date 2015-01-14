/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.preferences;

import com.openshift.client.IHttpClient;

/**
 * @author Andre Dietisheim
 */
public interface IOpenShiftPreferenceConstants {

	/** the timeout that's the client's using when reading the OpenShift response */ 
	public static final String CLIENT_READ_TIMEOUT = IHttpClient.SYSPROP_OPENSHIFT_READ_TIMEOUT;
	/** snapshot files */
	
	/**
	 * The list of hosts for kubernetes
	 */
	public static final String KUBERNETES_CONNECTIONS_KEY = "org.jboss.tools.openshift.express.KUBERNETES_CONNECTIONS";

}

