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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.jboss.tools.openshift.common.core.connection.ConnectionsFactory;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.common.core.connection.IConnectionsFactory;
import org.jboss.tools.openshift.test.core.connection.ConnectionFake;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andre Dietisheim
 */
public class ConnectionsFactoryTest {

	private static String A_CONNECTION_SERVER = "https://localhost:8080";
	private static String A_CONNECTION_FACTORY_ID = createConnectionFactoryId(AConnection.class);
	private static String OTHER_CONNECTION_SERVER = "https://localhost:8181";
	private static String OTHER_CONNECTION_FACTORY_ID = OtherConnection.class.getSimpleName() + "ConnectionFactory";

	private IConnectionsFactory connectionsFactory;
	
	@Before
	public void setUp() {
		this.connectionsFactory = new ConnectionsFactory();
		((ConnectionsFactory) connectionsFactory)
				.addConnectionFactory(createConnectionFactory(A_CONNECTION_FACTORY_ID, AConnection.class, A_CONNECTION_SERVER));
		((ConnectionsFactory) connectionsFactory)
				.addConnectionFactory(createConnectionFactory(OTHER_CONNECTION_FACTORY_ID, OtherConnection.class, OTHER_CONNECTION_SERVER));
	}
	
	@Test
	public void getAllShouldReturnAllFactories() {
		// pre-condition
		// operation
		// verification
		assertThat(connectionsFactory.getAll()).hasSize(2);
	}

	@Test
	public void addShouldAddAFactory() {
		// pre-condition
		ConnectionsFactory connectionsFactory = new ConnectionsFactory();
		Collection<IConnectionFactory> allFactories = connectionsFactory.getAll();
		assertThat(allFactories).isEmpty();
	
		// operation
		((ConnectionsFactory) connectionsFactory).addConnectionFactory(createConnectionFactory(A_CONNECTION_FACTORY_ID, AConnection.class, "http://localhost"));
		
		// verification
		assertThat(connectionsFactory.getAll()).hasSize(1);
	}

	@Test
	public void getByConnectionShouldReturnFactoryForGivenConnectionClass() {
		// pre-condition
		// operation
		IConnectionFactory aConnectionFactory = connectionsFactory.getByConnection(AConnection.class);

		// verification
		assertThat(aConnectionFactory).isNotNull();
		assertThat(aConnectionFactory.canCreate(AConnection.class)).isTrue();
	}

	@Test
	public void getAllShouldReturnFactoriesForGivenConnectionClass() {
		// pre-condition
		class DifferentConnection extends AConnection {

			public DifferentConnection(String host) {
				super(host);
			}
			
		}
		((ConnectionsFactory) connectionsFactory).addConnectionFactory(createConnectionFactory(createConnectionFactoryId(DifferentConnection.class), DifferentConnection.class, OTHER_CONNECTION_SERVER));
		assertThat(connectionsFactory.getAll()).hasSize(3);
		
		// operation
		Collection<IConnectionFactory> aConnectionFactories = connectionsFactory.getAll(AConnection.class);

		// verification
		assertThat(aConnectionFactories).hasSize(2);
	}

	@Test
	public void getFactoryShouldReturnFactoryForGivenHost() throws IOException {
		// pre-condition
		// operation
		IConnectionFactory aConnectionFactory = connectionsFactory.getFactory(A_CONNECTION_SERVER);

		// verification
		assertThat(aConnectionFactory).isNotNull();
		assertThat(aConnectionFactory.getDefaultHost()).isEqualTo(A_CONNECTION_SERVER);
	}

	@Test
	public void createShouldReturnFactoryForGivenHost() throws IOException {
		// pre-condition
		// operation
		IConnection connection = connectionsFactory.create(A_CONNECTION_SERVER);

		// verification
		assertThat(connection).isNotNull();
		assertThat(connection.getClass()).isEqualTo(AConnection.class);
	}

	private IConnectionFactory createConnectionFactory(final String id, final Class<?> clazz, final String host) {
		return new IConnectionFactory() {

			@Override
			public String getName() {
				return clazz.getSimpleName();
			}

			@Override
			public String getId() {
				return id;
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
			public <T extends IConnection> boolean canCreate(Class<T> otherClazz) {
				return otherClazz.isAssignableFrom(clazz);
			}

			@Override
			public String getUserDocUrl() {
				return null;
			}

			@Override
			public String getUserDocText() {
				return null;
			}
};
	}

	private static String createConnectionFactoryId(Class<? extends IConnection> clazz) {
		return clazz.getSimpleName() + "ConnectionFactory";
	}

	private static class AConnection extends ConnectionFake {

		public AConnection(String host) {
			super(host);
		}

		@Override
		public boolean canConnect() throws IOException {
			return A_CONNECTION_SERVER.equals(getHost());
		}
	}

	private static class OtherConnection extends ConnectionFake {

		public OtherConnection(String host) {
			super(host);
		}

		@Override
		public boolean canConnect() throws IOException {
			return OTHER_CONNECTION_SERVER.equals(getHost());
		}
	}
}
