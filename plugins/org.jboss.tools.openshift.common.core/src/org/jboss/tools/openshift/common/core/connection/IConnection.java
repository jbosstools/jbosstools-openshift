/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.core.connection;

import java.io.IOException;


/**
 * @author Jeff Cantrill
 * @author Andre Dietisheim
 */
public interface IConnection {

	public static final String PROPERTY_USERNAME = "username";
	public static final String PROPERTY_PASSWORD = "password";
	public static final String PROPERTY_REMEMBER_PASSWORD = "rememberPassword";
	public static final String PROPERTY_AUTHSCHEME = "authtype";
	public static final String PROPERTY_REMEMBER_TOKEN = "rememberToken";

	public String getHost();
	
	public String getUsername();

	public void setUsername(String username);

	public String getPassword();

	public void setPassword(String password);

	public void setRememberPassword(boolean rememberPassword);
	
	public boolean isRememberPassword();
	
	public boolean isDefaultHost();

	public String getScheme();
	
	public ConnectionType getType();

	public void refresh();

	public boolean canConnect() throws IOException;
	
	public boolean connect();
	
	public IConnection clone();

	public void update(IConnection connection);

	public void notifyUsage();

	public void enablePromptCredentials(boolean enable);

	public boolean isEnablePromptCredentials();

	/**
	 * Returns true if and only if connections are equal and
	 * credentials like userName and password are equal. 
	 * Default implementation compares userName and password.
	 * Other implementations may override it to compare other data, e.g. token.
	 * @param connection
	 * @return
	 */
	public boolean credentialsEqual(IConnection connection);
}
