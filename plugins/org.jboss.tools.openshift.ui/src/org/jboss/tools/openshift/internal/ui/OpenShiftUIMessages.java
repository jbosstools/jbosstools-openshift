/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import org.eclipse.osgi.util.NLS;

/**
 * UI static message
 * @author jeff.cantrill
 *
 */
public class OpenShiftUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages"; //$NON-NLS-1$
	static {
		NLS.initializeMessages(BUNDLE_NAME, OpenShiftUIMessages.class);
	}
	
	private OpenShiftUIMessages() {
	}
	
	public static String Name;
	public static String DisplayName;
	public static String Description;
	public static String ProjectDeletionConfirmation;
	public static String ProjectDeletionDialogTitle;
	
	public static String OCBinaryLocationIncompatibleErrorMessage;
	public static String OCBinaryLocationDontExistsErrorMessage;
	public static String NoOCBinaryLocationErrorMessage;
	public static String OCBinaryErrorMessage;
	public static String OCBinaryWarningMessage;
}
