/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.KEY_PASSWORD;
import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.KEY_SERVER;
import static org.jboss.tools.openshift.reddeer.utils.SystemProperties.KEY_USERNAME;

import java.util.Random;

/**
 * 
 * Storage of credentials and user data.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class DatastoreOS2 {

	// openshift enterprise
	public static final String KEY_X_SERVER = "openshift.xserver";

	public static String X_SERVER = "https://openshift.redhat.com";
	public static String X_USERNAME = "equo@mail.muni.cz";
	public static String X_PASSWORD = "rhqetestjbds19";
	public static String X_DOMAIN = "xjbdsqedomain" + new Random().nextInt(100);

	// configured server
	public static String SERVER = System.getProperty(KEY_SERVER); 
	public static String USERNAME = System.getProperty(KEY_USERNAME);
	public static String PASSWORD = System.getProperty(KEY_PASSWORD);
	public static String DOMAIN = "jbdstestdomain" + new Random().nextInt(100);
	public static String SECOND_DOMAIN = "secondjbdsqe" + new Random().nextInt(100);
	
	public static String SSH_HOME;
	public static String SSH_KEY_NAME = "MyKey";
	public static String SSH_KEY_FILENAME = "OpShKey" + System.currentTimeMillis(); 

	private DatastoreOS2() {}
}
