/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public class CreateDevfileRegistryModel extends OdoModel {
	public static final String PROPERTY_NAME = "name";
	public static final String PROPERTY_URL = "URL";
	public static final String PROPERTY_TOKEN = "token";

	private String name;
	private String url;
	private String token;

	public CreateDevfileRegistryModel(Odo odo) {
		super(odo);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		firePropertyChange(PROPERTY_NAME, this.name, this.name = name);
	}

	/**
	 * @return the url
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setURL(String url) {
		firePropertyChange(PROPERTY_URL, this.url, this.url = url);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		firePropertyChange(PROPERTY_TOKEN, this.token, this.token = token);
	}
}
