/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core;

import org.eclipse.osgi.util.NLS;

public class OpenShiftCoreMessages extends NLS {
	
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.core.OpenShiftCoreMessages"; //$NON-NLS-1$

	public static String DebugOnOpenshift;
	public static String ProfileOnOpenshift;
	public static String RunOnOpenshift;
	public static String OCBinaryPreferencesLink;
	public static String OCBinaryPreferencesDeactivatedLink;
	public static String OCBinaryDownloadDeactivatedLink;
	public static String OCBinaryLocationIncompatibleErrorMessage;
	public static String OCBinaryLinuxRSyncPermErrorMessage;
	public static String OCBinaryLocationRSyncWithSpaceErrorMessage;
	public static String OCBinaryLocationDontExistsErrorMessage;
	public static String NoOCBinaryLocationErrorMessage;
	public static String OCBinaryNotRecognized;
	public static String RsyncNotFoundMessage;
	public static String RsyncFstabNotFoundMessage;
	public static String RsyncFstabNotFoundDetailedMessage;
	public static String RsyncFstabInvalidMessage;
	public static String RsyncFstabInvalidDetailedMessage;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, OpenShiftCoreMessages.class);
	}

	private OpenShiftCoreMessages() {
	}
}
