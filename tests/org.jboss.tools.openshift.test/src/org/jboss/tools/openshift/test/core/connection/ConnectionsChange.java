/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionsRegistryListener;

public class ConnectionsChange {

	private static final long MAX_TIMEOUT_SEC = 10;

	private IConnection notifiedConnection;

	private boolean additionNotified;
	private boolean removalNotified;
	private boolean changeNotified;
	private String property;
	private CountDownLatch latch; 
	
	public String getProperty() {
		return property;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public Object getNewValue() {
		return newValue;
	}

	private Object oldValue;
	private Object newValue;

	public ConnectionsChange(ConnectionsRegistry registry) {
		registry.addListener(new Listener());
	}
	
	public void setCountDown(int totCallbacks) {
		this.latch = new CountDownLatch(totCallbacks);
	}
	
	public boolean isAdditionNotified() {
		return additionNotified;
	}

	public boolean isRemovalNotified() {
		return removalNotified;
	}

	public boolean isChangeNotified() {
		return changeNotified;
	}

	public IConnection getConnection() {
		return notifiedConnection;
	}
	
	public void reset() {
		this.additionNotified = false;
		this.removalNotified = false;
		this.changeNotified = false;
		this.notifiedConnection = null;
		this.latch = null;
	}
	
	private void bumpCountdown() {
		if(latch != null) {
			latch.countDown();
		}
	}
	
	public void waitForNotification() {
		if(latch == null) {
			return;
		}
		try {
			if(!latch.await(MAX_TIMEOUT_SEC, TimeUnit.SECONDS)) {
				throw new RuntimeException("ConnectionsChange timed out before receiving any notifications");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private class Listener implements IConnectionsRegistryListener {

		@Override
		public void connectionAdded(IConnection connection) {
			additionNotified = true;
			notifiedConnection = connection;
			bumpCountdown();
		}

		@Override
		public void connectionRemoved(IConnection connection) {
			removalNotified = true;
			notifiedConnection = connection;
			bumpCountdown();
		}

		@Override
		public void connectionChanged(IConnection connection, String eventProperty, Object eventOldValue, Object eventNewValue) {
			bumpCountdown();
			changeNotified = true;
			notifiedConnection = connection;
			property = eventProperty;
			oldValue = eventOldValue;
			newValue = eventNewValue;
		}
	}
}