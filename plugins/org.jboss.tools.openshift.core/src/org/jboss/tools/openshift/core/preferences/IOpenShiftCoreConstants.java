/*******************************************************************************
 * Copyright (c) 2015-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.preferences;

/**
 * @author jeff.cantrill
 */
public interface IOpenShiftCoreConstants {

	static final String OPENSHIFT_CLI_LOC = "openshift.cli.location";

	/**
	 * URL of the web page for OpenShift downloads.
	 */
	public static final String DOWNLOAD_URL = "https://www.okd.io/download.html";

	/**
	 * URL of the web page with OpenShift instructions.
	 */
	public static final String INSTALLATION_INSTRUCTIONS_URL = "https://docs.okd.io/latest/cli_reference/get_started_cli.html";

	/**
	 * Identifier of the OpenShift preference page.
	 */
	public static final String OPEN_SHIFT_PREFERENCE_PAGE_ID = "org.jboss.tools.openshift.ui.preferences.OpenShiftPreferencePage";

}
