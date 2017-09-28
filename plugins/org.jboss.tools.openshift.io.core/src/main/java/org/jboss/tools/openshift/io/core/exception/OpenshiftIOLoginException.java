/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core.exception;

import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;

public class OpenshiftIOLoginException extends OpenshiftIOException {

	private final ICluster cluster;
	
	private final IAccount account;
	
	public OpenshiftIOLoginException(ICluster cluster, IAccount account) {
		super("Error login to cluster " + cluster.getId());
		this.cluster = cluster;
		this.account = account;
		
	}

	/**
	 * @return the cluster
	 */
	public ICluster getCluster() {
		return cluster;
	}

	/**
	 * @return the account
	 */
	public IAccount getAccount() {
		return account;
	}
}
