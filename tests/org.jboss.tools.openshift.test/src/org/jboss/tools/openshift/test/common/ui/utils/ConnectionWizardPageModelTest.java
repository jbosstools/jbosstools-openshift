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
package org.jboss.tools.openshift.test.common.ui.utils;

import static org.apache.commons.collections.ListUtils.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author Andre Dietisheim
 */
public class ConnectionWizardPageModelTest {

	private ConnectionWizardPageModel model;
	@Mock
	private IConnectionAware<IConnection> wizardModel;

	private String[] allHosts = new String[] {
			"https://www.redhat.com", "http://openshift.com", "https://10.1.2.2:8443" };
	
	private Connection editedConnection = mockOS3Connection("andre.dietisheim@chapeuvermelho.pt", allHosts[0]);
	
	private List<Connection> os3Connections = Arrays.asList(
			mockOS3Connection("adietish@roterhut.ch", allHosts[1]),
			mockOS3Connection("adietish@chapeaurouge.ch", allHosts[2])
			);
	private List<IConnection> otherConnections = Arrays.asList(
			mockOtherConnection("adietish@cappellorosso.ch", allHosts[1]),
			mockOtherConnection("adietish@sombrerorojo.es", allHosts[2])
			);
	
	@SuppressWarnings("unchecked")
	private List<IConnection> allConnections = 	union(os3Connections, otherConnections);
	
	@Before
	public void setUp() {
		this.model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				true,
				wizardModel); 
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getAllConnectionsReturnsAllConnectionsAndNewConnectionMarkerIfConnectionTypeIsNullAndConnectionChangeIsAllowed() {
		// pre-condition
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				true,
				wizardModel); 
		// operation
		// verification
		assertThat(model.getAllConnections()).containsExactlyElementsOf(
				union(Collections.singletonList(NewConnectionMarker.getInstance()), allConnections));
	}
	
	@Test
	public void getAllConnectionsReturnsOnlyEditedConnectionAndNewConnectionMarkerIfConnectionChangeIsDisallowed() {
		// pre-condition
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				false,
				wizardModel); 
		// operation
		// verification
		assertThat(model.getAllConnections()).containsExactlyElementsOf(
				Arrays.asList(NewConnectionMarker.getInstance(), editedConnection));
	}

	private Connection mockOS3Connection(String username, String url) {
		Connection mock = mock(Connection.class);
		when(mock.getHost()).thenReturn(url);
		when(mock.getUsername()).thenReturn(username);
		return mock;
	}

	private IConnection mockOtherConnection(String username, String url) {
		IConnection mock = mock(IConnection.class);
		when(mock.getHost()).thenReturn(url);
		when(mock.getUsername()).thenReturn(username);
		return mock;
	}

	private static class TestableConnectionWizardPageModel extends ConnectionWizardPageModel {

		public TestableConnectionWizardPageModel(IConnection editedConnection, Collection<IConnection> allConnections,
				Class<? extends IConnection> connectionType, boolean allowConnectionChange,
				IConnectionAware<IConnection> wizardModel) {
			super(editedConnection, allConnections, connectionType, allowConnectionChange, wizardModel);
		}
	}

}
