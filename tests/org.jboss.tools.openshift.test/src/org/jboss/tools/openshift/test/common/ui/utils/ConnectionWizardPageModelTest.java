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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.tools.openshift.common.core.ICredentialsPrompter;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.openshift.restclient.ISSLCertificateCallback;

/**
 * @author Andre Dietisheim
 */
public class ConnectionWizardPageModelTest {

	@Mock
	private IConnectionAware<IConnection> wizardModel;
	private String[] allHosts = new String[] {
			"https://www.redhat.com", "http://openshift.com", "https://10.1.2.2:8443" };
	private Connection editedConnection = mockOS3Connection("andre.dietisheim@chapeuvermelho.pt", allHosts[0]);
	private List<Connection> os3Connections = Arrays.asList(
				mockOS3Connection("adietish@roterhut.ch", allHosts[0]),
				mockOS3Connection("adietish@chapeaurouge.ch", allHosts[1]),
				mockOS3Connection("adietish@redhat.ch", allHosts[2])
			);
	private List<IConnection> otherConnections = Arrays.asList(
				mockOtherConnection("adietish@cappellorosso.ch", allHosts[0]),
				mockOtherConnection("adietish@sombrerorojo.es", allHosts[1])
			);
	
	@SuppressWarnings("unchecked")
	private List<IConnection> allConnections = 	union(os3Connections, otherConnections);

	private TestableConnectionWizardPageModel model;

	@Before
	public void setUp() {
		this.model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				editedConnection.getClass(),
				true,
				wizardModel);

	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void getAllConnections_should_return_all_connections_and_NewConnectionMarker_if_connection_type_is_null_and_connection_change_is_allowed() {
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
	public void getAllConnections_should_return_only_edited_connection_and_NewConnectionMarker_if_connection_change_is_disallowed() {
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

	@Test
	public void getAllHosts_should_return_all_hosts_within_all_connections() {
		// pre-condition
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				false,
				wizardModel); 
		// operation
		Collection<String> allHosts = model.getAllHosts();

		// verification
		assertThat(allHosts).containsOnly(this.allHosts);
	}

	@Test
	public void getAllHosts_should_only_contain_unique_hosts() {
		// pre-condition
		assertThat(allConnections).isNotEmpty();
		allConnections.add(allConnections.get(0)); // add duplicate host
		
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				false,
				wizardModel); 
		// operation
		Collection<String> allHosts = model.getAllHosts();

		// verification
		assertThat(allHosts).containsOnly(this.allHosts);
	}

	@Test
	public void should_edit_new_connection_given_edited_connection_is_null() {
		// pre-condition
		TestableConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				null, 
				allConnections,
				null,
				true,
				wizardModel); 
		// operation
		boolean isNewConnection = model.isNewConnection();
		// verification
		assertThat(isNewConnection).isTrue();
	}

	@Test
	public void should_edit_existing_connection_given_edited_connection_is_not_null_but_wont_match_connection_type() {
		// pre-condition
		TestableConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				null,
				true,
				wizardModel); 
		// operation
		boolean isNewConnection = model.isNewConnection();
		// verification
		assertThat(isNewConnection).isFalse();
	}

	@Test
	public void should_edit_existing_connection_given_edited_connection_is_not_null() {
		// pre-condition
		TestableConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				editedConnection.getClass(),
				true,
				wizardModel);
		// operation
		boolean isNewConnection = model.isNewConnection();
		// verification
		assertThat(isNewConnection).isFalse();
	}

	@Test
	public void should_edit_new_connection_given_edited_connection_is_not_null_but_wont_match_connection_type() {
		// pre-condition
		TestableConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				editedConnection, 
				allConnections,
				OtherConnection.class,
				true,
				wizardModel);
		// operation
		boolean isNewConnection = model.isNewConnection();
		// verification
		assertThat(isNewConnection).isTrue();
	}

	@Test
	public void should_disable_prompt_on_edited_connection() {
		// given
		// when
		// then
		verify(editedConnection).enablePromptCredentials(false);
	}
	
	@Test
	public void should_enable_promptCredentials_on_edited_connection_when_disposing_model_and_was_enabled_before_editing_it() {
		// given prompt is disabled once connection is being edited
		Connection connection = mockOS3Connection("foo", "https://localhost");
		doReturn(true).when(connection).isEnablePromptCredentials();
		allConnections.add(connection);
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				connection, 
				allConnections,
				editedConnection.getClass(),
				true,
				wizardModel);
		verify(connection).enablePromptCredentials(false);
		// when
		model.dispose();
		// then
		verify(connection).enablePromptCredentials(true);
	}

	@Test
	public void should_disable_promptCredentials_on_edited_connection_when_disposing_model_and_was_disabled_before_editing_it() {
		// given prompt is disabled once connection is being edited
		Connection connection = mockOS3Connection("foo", "https://localhost");
		doReturn(false).when(connection).isEnablePromptCredentials();
		allConnections.add(connection);
		ConnectionWizardPageModel model = new TestableConnectionWizardPageModel(
				connection, 
				allConnections,
				connection.getClass(),
				true,
				wizardModel);
		verify(connection).enablePromptCredentials(false);
		// when
		model.dispose();
		// then disable 2x, once when editing, 2nd time when disposing
		verify(connection, times(2)).enablePromptCredentials(false);
	}

	private Connection mockOS3Connection(String username, String url) {
		Connection mock = mock(Connection.class);
		when(mock.getHost()).thenReturn(url);
		when(mock.getUsername()).thenReturn(username);
		when(mock.isEnablePromptCredentials()).thenReturn(true);
		return mock;
	}

	private IConnection mockOtherConnection(String username, String url) {
		IConnection mock = mock(IConnection.class);
		when(mock.getHost()).thenReturn(url);
		when(mock.getUsername()).thenReturn(username);
		return mock;
	}

	private class OtherConnection extends Connection {

		public OtherConnection(String url, ICredentialsPrompter credentialsPrompter,
				ISSLCertificateCallback sslCertCallback) throws MalformedURLException {
			super(url, credentialsPrompter, sslCertCallback);
		}
	}

	private static class TestableConnectionWizardPageModel extends ConnectionWizardPageModel {

		public TestableConnectionWizardPageModel(IConnection editedConnection, Collection<IConnection> allConnections,
				Class<? extends IConnection> connectionType, boolean allowConnectionChange,
				IConnectionAware<IConnection> wizardModel) {
			super(editedConnection, allConnections, connectionType, allowConnectionChange, wizardModel);
		}

		@Override
		public boolean isNewConnection() {
			return super.isNewConnection();
		}
	}

}
