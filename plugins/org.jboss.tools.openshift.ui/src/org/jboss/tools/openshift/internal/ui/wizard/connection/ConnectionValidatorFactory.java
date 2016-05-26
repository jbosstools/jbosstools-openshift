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
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel;

import com.openshift.restclient.authorization.IAuthorizationContext;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class ConnectionValidatorFactory {

	/**
	 * Returns new validator that checks that there is no connection other than the selected one
	 * with host and username provided by the observable values.
	 * @param pageModel
	 * @param usernameObservable
	 * @param urlObservable
	 * @return
	 */
	public static MultiValidator createBasicAuthenticationValidator(ConnectionWizardPageModel pageModel, IObservableValue usernameObservable, IObservableValue<?> urlObservable) {
		return new MultiValidator() {
			@Override
			protected IStatus validate() {
				String user1 = (String)usernameObservable.getValue();
				String mHost = (String)urlObservable.getValue();
				IConnection current = pageModel.getSelectedConnection();
				for (Connection c: ConnectionsRegistrySingleton.getInstance().getAll(Connection.class)) {
					if(c != current && IAuthorizationContext.AUTHSCHEME_BASIC.equals(c.getAuthScheme())) {
						String host = c.getHost();
						String user = c.getUsername();
						if(host != null && host.equals(mHost) && user != null && user.equals(user1)) {
							return ValidationStatus.error("Connection for the server with this username already exists.");
						}
					}
				}
				return ValidationStatus.ok();
			}
		};
	}

	/**
	 * Returns new validator that checks that there is no connection other than the selected one
	 * with host and token provided by the observable values.
	 * @param pageModel
	 * @param tokenObservable
	 * @param urlObservable
	 * @return
	 */
	public static MultiValidator createOAuthAuthenticationValidator(ConnectionWizardPageModel pageModel, IObservableValue tokenObservable, IObservableValue<?> urlObservable) {
		return new MultiValidator() {
			@Override
			protected IStatus validate() {
				String token1 = (String)tokenObservable.getValue();
				String mHost = (String)urlObservable.getValue();
				IConnection current = pageModel.getSelectedConnection();
				for (Connection c: ConnectionsRegistrySingleton.getInstance().getAll(Connection.class)) {
					if(c != current && IAuthorizationContext.AUTHSCHEME_OAUTH.equals(c.getAuthScheme())) {
						String host = c.getHost();
						String token = c.getToken();
						if(host != null && host.equals(mHost) && token != null && token.equals(token1)) {
							return ValidationStatus.error("Connection for the server with this token already exists.");
						}
					}
				}
				return ValidationStatus.ok();
			}
		};
	}
}
