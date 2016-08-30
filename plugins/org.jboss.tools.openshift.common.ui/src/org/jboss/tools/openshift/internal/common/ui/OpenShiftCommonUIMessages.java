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
package org.jboss.tools.openshift.internal.common.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Common UI static message
 * @author Jeff Maury
 *
 */
public class OpenShiftCommonUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIMessages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, OpenShiftCommonUIMessages.class);
	}
	
	private OpenShiftCommonUIMessages() {
	}
	
    public static String GeneralProjectWarningMessage;
	public static String MavenProjectWarningMessage;
	public static String MavenProjectsWarningMessage;
	public static String OverwriteProjectsDialogTitle;
	
	public static String ImportButtonLabel;
	
}
