/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.preferences;

/**
 * @author Andre Dietisheim
 */
public interface IOpenShiftPreferenceConstants {

	/** available connections */
	public static final String CONNECTIONS = "org.jboss.tools.openshift.express.CONNECTION_NAMES";
	/** the prefs key used in prior versions */
	public static final String RHLOGIN_LIST_PREFS_KEY = "org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardModel_RHLOGIN_LIST";
	/** last user name */
	public static final String LAST_USERNAME = "org.jboss.tools.openshift.express.LAST_USERNAME";
	/** server history */
	public static final String SERVERS = "org.jboss.tools.openshift.express.SERVERS";
	/** default server */
	public static final String DEFAULT_HOST = "org.jboss.tools.openshift.express.SERVER";
	/** tail command */
	public static final String TAIL_FILE_OPTIONS = "org.jboss.tools.openshift.express.TAILFILEOPTIONS";

}
