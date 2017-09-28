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

import org.jboss.tools.openshift.io.core.model.IAccount;

public class OpenshiftIORefreshException extends OpenshiftIOException {

	private final IAccount account;
	private final int status;
	
	public OpenshiftIORefreshException(IAccount account, int status) {
		super("Error refreshing token for " + account.getId() + " on cluster " + account.getCluster().getId());
		this.account = account;
		this.status = status;
	}

	public OpenshiftIORefreshException(IAccount account, Throwable t) {
		super("Error refreshing token for " + account.getId() + " on cluster " + account.getCluster().getId(), t);
		this.account = account;
		this.status = (-1);
	}

	/**
	 * @return the account
	 */
	public IAccount getAccount() {
		return account;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}
}
