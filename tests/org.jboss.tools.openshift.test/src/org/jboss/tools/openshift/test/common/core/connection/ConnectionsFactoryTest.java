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
package org.jboss.tools.openshift.test.common.core.connection;

import java.io.IOException;

import org.jboss.tools.openshift.common.core.connection.ConnectionsFactory;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.test.core.connection.ConnectionFake;

/**
 * @author Andre Dietisheim
 */
public class ConnectionsFactoryTest {

	private static String ONE_CONNECTION_SERVER = "https://localhost:8080";
	private static String OTHER_CONNECTION_SERVER = "https://localhost:8181";

	private ConnectionsFactory connectionsFactory;

	public void setUp() {
		this.connectionsFactory = new ConnectionsFactory();
		connectionsFactory.addConnectionFactory(createConnectionFactory(OneConnection.class, ONE_CONNECTION_SERVER));
		connectionsFactory
				.addConnectionFactory(createConnectionFactory(OtherConnection.class, OTHER_CONNECTION_SERVER));
	}

	private IConnectionFactory createConnectionFactory(final Class<?> clazz, final String host) {
		return new IConnectionFactory() {

			@Override
			public String getName() {
				return clazz.getSimpleName();
			}

			@Override
			public String getId() {
				return clazz.getName();
			}

			@Override
			public IConnection create(String host) {
				try {
					return (IConnection) clazz.getConstructor(String.class).newInstance(host);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getDefaultHost() {
				return host;
			}

			@Override
			public boolean hasDefaultHost() {
				return true;
			}

			@Override
			public <T extends IConnection> boolean canCreate(Class<T> clazz) {
				return clazz.equals(clazz);
			}

			@Override
			public String getSignupUrl(String host) {
				return null;
			}

			@Override
			public String getUserDocUrl() {
				return null;
			}
		};
	}

	private class OneConnection extends ConnectionFake {

		public OneConnection(String host) {
			super(host);
		}

		@Override
		public boolean canConnect() throws IOException {
			return ONE_CONNECTION_SERVER.equals(getHost());
		}
	}

	private class OtherConnection extends ConnectionFake {

		public OtherConnection(String host) {
			super(host);
		}

		@Override
		public boolean canConnect() throws IOException {
			return OTHER_CONNECTION_SERVER.equals(getHost());
		}
	}
}
