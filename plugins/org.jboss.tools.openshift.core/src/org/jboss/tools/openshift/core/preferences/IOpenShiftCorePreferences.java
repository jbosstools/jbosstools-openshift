/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.core.preferences;

import java.util.Map;

/**
 * OpenShift core preferences
 * 
 * @author jeff.cantrill
 *
 */
public interface IOpenShiftCorePreferences {

	String[] loadConnections() ;

	void saveConnections(String[] connections);
	
	void saveAuthScheme(String connectionURL, String scheme);

	void removeAuthScheme(String connectionURL);
	
	String loadScheme(String connectionURL);

	String getOCBinaryLocation();
	
	void saveOCBinaryLocation(String location);
	
	/**
	 * Save the extended properties of a connection.  This will
	 * create a preference key by connectionURL
	 * @param connectionUrl
	 * @param ext
	 */
	void saveExtProperties(String connectionURL, Map<String, Object> ext);
	
	/**
	 * Load the extended properties of a connection
	 * @param connectionURL
	 * @return
	 */
	Map<String, Object> loadExtProperties(String connectionURL);
}