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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccountModel;
import org.jboss.tools.openshift.io.core.model.IAccountModelListener;
import org.jboss.tools.openshift.io.core.model.AccountModelAdapter;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.junit.Test;

public class AccountModelTest {

	@Test
	public void testAddRemoveCluster() {
		IAccountModel m = AccountService.getDefault().getModel();
		assertNotNull(m);

		final Boolean[] checkAdded = new Boolean[] { false };
		final Boolean[] checkRemoved = new Boolean[] { false };

		IAccountModelListener listener = new AccountModelAdapter() {
			@Override
			public void clusterAdded(IAccountModel source, ICluster cluster) {
				checkAdded[0] = true;
			}

			@Override
			public void clusterRemoved(IAccountModel source, ICluster cluster) {
				checkRemoved[0] = true;
			}
		};
		m.addListener(listener);

		try {
			ICluster cluster = m.createCluster("id1");
			m.addCluster(cluster);
			List<ICluster> clusters = m.getClusters();
			assertEquals(2, clusters.size());
			m.removeCluster(cluster);
			clusters = m.getClusters();
			assertEquals(1, clusters.size());

			assertTrue(checkAdded[0]);
			assertTrue(checkRemoved[0]);
		} finally {
			m.removeListener(listener);
		}
	}

	@Test
	public void testAddRemoveAccount() {
		IAccountModel m = AccountService.getDefault().getModel();
		assertNotNull(m);

		final Boolean[] checkAdded = new Boolean[] { false };
		final Boolean[] checkRemoved = new Boolean[] { false };

		IAccountModelListener listener = new AccountModelAdapter() {
			@Override
			public void accountAdded(IAccountModel source, IAccount account) {
				checkAdded[0] = true;
			}

			@Override
			public void accountRemoved(IAccountModel source, IAccount account) {
				checkRemoved[0] = true;
			}
		};
		m.addListener(listener);

		try {
			ICluster cluster = m.createCluster("id1");
			m.addCluster(cluster);
			List<ICluster> clusters = m.getClusters();
			assertEquals(2, clusters.size());
			assertEquals(0, cluster.getAccounts().size());
			IAccount account = cluster.createAccount("id2");
			cluster.addAccount(account);
			assertEquals(1, cluster.getAccounts().size());
			cluster.removeAccount(account);
			assertEquals(0, cluster.getAccounts().size());
			m.removeCluster(cluster);
			clusters = m.getClusters();
			assertEquals(1, clusters.size());

			assertTrue(checkAdded[0]);
			assertTrue(checkRemoved[0]);
		} finally {
			m.removeListener(listener);
		}
	}
}
