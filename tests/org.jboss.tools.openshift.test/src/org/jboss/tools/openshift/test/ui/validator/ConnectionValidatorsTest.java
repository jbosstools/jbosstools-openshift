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
package org.jboss.tools.openshift.test.ui.validator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;
import org.jboss.tools.openshift.internal.ui.wizard.connection.BasicAuthenticationDetailView;
import org.jboss.tools.openshift.internal.ui.wizard.connection.ConnectionValidatorFactory;
import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthDetailView;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionValidatorsTest {
	static final String HOST1 = "http://host1";
	static final String HOST2 = "http://host2";
	static final String HOST3 = "http://host3";

	static final String USER1 = "user1";
	static final String USER2 = "user2";
	static final String USER3 = "user3";

	static final String TOKEN1 = "token1";
	static final String TOKEN2 = "token2";
	static final String TOKEN3 = "token3";

	ConnectionsRegistry registry;
	List<Connection> connections;

	@Before
	public void init() {
		registry = ConnectionsRegistrySingleton.getInstance();
		connections = new ArrayList<>();
	}

	@Test
	public void testBasicAuthenticationValidator() {
		Connection connection1 = mockConnection(HOST1, USER1, null);
		mockConnection(HOST2, USER2, null);

		ConnectionWizardPageModel pageModel = mockConnectionWizardPageModel(connection1);

		WritableValue<String> usernameObservable = new WritableValue<String>();
		WritableValue<String> urlObservable = new WritableValue<String>();
		MultiValidator v = ConnectionValidatorFactory.createBasicAuthenticationValidator(pageModel, usernameObservable, urlObservable);

		v.observeValidatedValue(urlObservable);

		//New connection
		urlObservable.setValue(HOST3);
		usernameObservable.setValue(USER3);
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));

		urlObservable.setValue(HOST2);
		//Host exists, but token is different
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));

		usernameObservable.setValue(USER2);
		//Existing not selected connection
		Assert.assertEquals(IStatus.ERROR, getStatusSeverity(v));

		//Selected connection
		urlObservable.setValue(HOST1);
		usernameObservable.setValue(USER1);
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));
	}

	@Test
	public void testBasicAuthenticationValidatorInUI() {
		Connection connection1 = mockConnection(HOST1, USER1, null);
		mockConnection(HOST2, USER2, null);

		ConnectionWizardPageModel pageModel = mockConnectionWizardPageModel(connection1);
		Mockito.when(pageModel.getHost()).thenReturn(HOST2);

		IValueChangeListener<Object> l = new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
			}
		};
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Composite composite = new Composite(shell, SWT.NONE);
		try {
			BasicAuthenticationDetailView view = new BasicAuthenticationDetailView(pageModel, l, null);
			DataBindingContext dbc = new DataBindingContext();
			view.createControls(shell, null, dbc);
			view.onVisible(null, dbc);
			view.getPasswordTextControl().setText("pass");
			view.getUsernameTextControl().setText(USER2);

			MultiValidator v = findValidator(dbc);

			Assert.assertEquals(IStatus.ERROR, getStatusSeverity(v));

			view.getUsernameTextControl().setText(USER3);
			Assert.assertEquals(IStatus.OK, getStatusSeverity(v));
		} finally {
			composite.dispose();
		}
	}

	private MultiValidator findValidator(DataBindingContext dbc) {
		MultiValidator result = null;
		for (Object o: dbc.getValidationStatusProviders()) {
			if(o instanceof MultiValidator) {
				Assert.assertNull("Multiple validators: " + result + ", " + o + ". Please improve the test to pick the right validator.", result);
				result = (MultiValidator)o;
			}
		}
		Assert.assertNotNull("Validator not found", result);
		return result;
	}

	@Test
	public void testOAuthAuthenticationValidator() {
		Connection connection1 = mockConnection(HOST1, null, TOKEN1);
		mockConnection(HOST2, null, TOKEN2);

		ConnectionWizardPageModel pageModel = mockConnectionWizardPageModel(connection1);

		WritableValue<String> tokenObservable = new WritableValue<String>();
		WritableValue<String> urlObservable = new WritableValue<String>();
		MultiValidator v = ConnectionValidatorFactory.createOAuthAuthenticationValidator(pageModel, tokenObservable, urlObservable);

		v.observeValidatedValue(urlObservable);

		//New connection
		urlObservable.setValue(HOST3);
		tokenObservable.setValue(TOKEN3);
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));

		urlObservable.setValue(HOST2);
		//Host exists, but token is different
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));

		tokenObservable.setValue(TOKEN2);
		//Existing not selected connection
		Assert.assertEquals(IStatus.ERROR, getStatusSeverity(v));

		//Selected connection
		urlObservable.setValue(HOST1);
		tokenObservable.setValue(TOKEN1);
		Assert.assertEquals(IStatus.OK, getStatusSeverity(v));
	}

	@Test
	public void testOAuthAuthenticationValidatorInUI() {
		Connection connection1 = mockConnection(HOST1, null, TOKEN1);
		mockConnection(HOST2, null, TOKEN2);

		ConnectionWizardPageModel pageModel = mockConnectionWizardPageModel(connection1);
		Mockito.when(pageModel.getHost()).thenReturn(HOST2);

		IValueChangeListener<Object> l = new IValueChangeListener<Object>() {
			@Override
			public void handleValueChange(ValueChangeEvent<? extends Object> event) {
			}
		};
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Composite composite = new Composite(shell, SWT.NONE);
		try {
			OAuthDetailView view = new OAuthDetailView(null, pageModel, l, null, null);
			DataBindingContext dbc = new DataBindingContext();
			view.createControls(shell, null, dbc);
			view.onVisible(null, dbc);
			view.getTokenTextControl().setText(TOKEN2);

			MultiValidator v = findValidator(dbc);

			Assert.assertEquals(IStatus.ERROR, getStatusSeverity(v));

			view.getTokenTextControl().setText(TOKEN3);
			Assert.assertEquals(IStatus.OK, getStatusSeverity(v));
		} finally {
			composite.dispose();
		}
	}

	/**
	 * Creates a connection instance and adds it to the registry.
	 * @param host
	 * @param username
	 * @param token
	 * @return
	 */
	private Connection mockConnection(String host, String username, String token) {
		boolean isOAuth = token != null;
		Connection connection = Mockito.mock(Connection.class);
		Mockito.when(connection.getHost()).thenReturn(host);
		Mockito.when(connection.getUsername()).thenReturn(username);
		Mockito.when(connection.getToken()).thenReturn(token);
		Mockito.when(connection.getAuthScheme())
			.thenReturn(isOAuth ? IAuthorizationContext.AUTHSCHEME_OAUTH : IAuthorizationContext.AUTHSCHEME_BASIC);
		registry.add(connection);
		connections.add(connection);
		return connection;
	}

	private ConnectionWizardPageModel mockConnectionWizardPageModel(Connection selected) {
		ConnectionWizardPageModel pageModel = Mockito.mock(ConnectionWizardPageModel.class);
		Mockito.when(pageModel.getSelectedConnection()).thenReturn(selected);
		return pageModel;
	}

	@After
	public void clear() {
		if(registry != null && !connections.isEmpty()) {
			connections.stream().forEach(c -> registry.remove(c));
		}
	}

	private int getStatusSeverity(MultiValidator v) {
		return ((IStatus)v.getValidationStatus().getValue()).getSeverity();
	}
}
