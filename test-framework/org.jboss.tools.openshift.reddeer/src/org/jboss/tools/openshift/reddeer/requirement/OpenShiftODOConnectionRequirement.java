/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.reddeer.exception.OpenShiftToolsException;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;

/**
 * Create connection in OPenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
public class OpenShiftODOConnectionRequirement implements Requirement<RequiredODOConnection> {

	private static Logger LOGGER = new Logger(OpenShiftODOConnectionRequirement.class);

	private RequiredODOConnection connectionSpec;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RequiredODOConnection {
		/**
		 * the host to connect to. If nothing is provided {@link DatastoreOS3#SERVER} is
		 * used
		 */
		String serverODO()

		default StringUtils.EMPTY;

		/**
		 * the username to use when authenticating. If nothing is provided
		 * {@link DatastoreOS3#USERNAME} is used
		 */
		String usernameODO()

		default StringUtils.EMPTY;

		/**
		 * the password to use when authenticating. If nothing is provided
		 * {@link DatastoreOS3#PASSWORD} is used
		 */
		String passwordODO()

		default StringUtils.EMPTY;

		/**
		 * the token to use when authenticating. If nothing is provided
		 * {@link DatastoreOS3#TOKEN} is used
		 */
		String tokenODO()

		default StringUtils.EMPTY;

		/**
		 * whether the connection created by the requirement should be automatically
		 * deleted after test class, default false
		 */
		boolean cleanup() default false;
	}

	@Override
	public void fulfill() {
		String server = TestUtils.getValueOrDefault(connectionSpec.serverODO(), DatastoreOS3.SERVER);
		String username = TestUtils.getValueOrDefault(connectionSpec.usernameODO(), DatastoreOS3.USERNAME);
		String password = TestUtils.getValueOrDefault(connectionSpec.passwordODO(), DatastoreOS3.PASSWORD);
		String token = TestUtils.getValueOrDefault(connectionSpec.tokenODO(), DatastoreOS3.TOKEN);

		try {
			OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
			explorer.open();
			if (explorer.connectionExistsAndWorking()) {
				LOGGER.debug("Connection exist and working to OpenShift server");
			} else {
				LOGGER.debug("No connection found. Creating a new one.");
				if (StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(username)) {
					explorer.connectToOpenShiftODOBasic(server, username, password);
				} else if (StringUtils.isNotEmpty(token)) {
					explorer.connectToOpenShiftODOOAuth(server, token);
				} else {
					throw new OpenShiftToolsException("Could not create connection");
				}
			}

		} catch (Exception s) {
			throw new OpenShiftToolsException("Cannot fulfill requirement");
		}

	}

	@Override
	public void setDeclaration(RequiredODOConnection connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	@Override
	public void cleanUp() {
		// nothing to do
	}

	@Override
	public RequiredODOConnection getDeclaration() {
		return this.connectionSpec;
	}
}
