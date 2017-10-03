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
package org.jboss.tools.openshift.io.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.junit.Test;

public class AccountServiceTest {

	private AccountService service = AccountService.getDefault();
	
	private ICluster cluster = service.getModel().getClusters().get(0);
	
	@Test
	public void checkModelIsAvailable() {
		assertNotNull(cluster);
	}
	
	@Test
	public void checkLoginIsRequiredIfNoAccessToken() {
		IAccount account = cluster.createAccount("myid");
		AccountStatus status = service.getStatus(account);
		assertEquals(AccountStatus.NEEDS_LOGIN, status);
	}


	@Test
	public void checkRefreshIsRequiredIfAccessTokenExpired() {
		IAccount account = cluster.createAccount("myid");
		account.setAccessToken("at");
		account.setRefreshToken("rt");
		account.setAccessTokenExpiryTime(System.currentTimeMillis() - 1000);
		AccountStatus status = service.getStatus(account);
		assertEquals(AccountStatus.NEEDS_REFRESH, status);
	}
	
	@Test
	public void checkRefreshIsRequiredIfLastAccessed2DaysAgo() {
		IAccount account = cluster.createAccount("myid");
		account.setAccessToken("at");
		account.setRefreshToken("rt");
		account.setLastRefreshedTime(System.currentTimeMillis() - 24 * 3600 * 2 * 1000);
		AccountStatus status = service.getStatus(account);
		assertEquals(AccountStatus.NEEDS_REFRESH, status);
	}
	
	@Test
	public void checkValidIfAccessedRecentry() {
		IAccount account = cluster.createAccount("myid");
		account.setAccessToken("at");
		account.setRefreshToken("rt");
		account.setLastRefreshedTime(System.currentTimeMillis());
		AccountStatus status = service.getStatus(account);
		assertEquals(AccountStatus.VALID, status);
	}
}
