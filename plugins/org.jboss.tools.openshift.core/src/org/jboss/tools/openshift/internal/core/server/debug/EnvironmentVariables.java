/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core.server.debug;

import org.apache.commons.lang.StringUtils;

import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IEnvironmentVariable;

/**
 * A class that allows to read and write environment variables of a given
 * deployment config.
 * 
 * @author Andre Dietisheim
 */
public class EnvironmentVariables {

	private IDeploymentConfig dc;

	EnvironmentVariables(IDeploymentConfig dc) {
		this.dc = dc;
	}

	/**
	 * Returns the boolean value for given key in the environment variables for the
	 * given deployment config.
	 * 
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(dc, key));
	}

	/**
	 * Returns the string value for given key in the environment variables of the
	 * given deployment config.
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		return get(dc, key);
	}

	private String get(IDeploymentConfig dc, String key) {
		if (dc == null || dc.getEnvironmentVariables() == null || StringUtils.isEmpty(key)) {
			return null;
		}
		return dc.getEnvironmentVariables().stream().filter(ev -> key.equals(ev.getName())).findFirst()
				.map(IEnvironmentVariable::getValue).orElse(null);
	}

	/**
	 * Sets the given value for the given key in the environment variables of the
	 * given deployment config.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		if (dc == null) {
			return;
		}
		dc.setEnvironmentVariable(key, value);
	}

	/**
	 * Removes the value and the given key in the environment variables for the
	 * given deployment config.
	
	 * @param key
	 */
	public void remove(String key) {
		dc.removeEnvironmentVariable(key);
	}

}
