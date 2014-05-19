/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.express.internal.ui;

import org.eclipse.osgi.util.NLS;

public class OpenshiftUIMessages extends NLS {
	
	private static final String BUNDLE_NAME = "org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages"; //$NON-NLS-1$

	static {
		NLS.initializeMessages(BUNDLE_NAME, OpenshiftUIMessages.class);
	}
	
	private OpenshiftUIMessages() {
	}

	public static String OpenshiftWizardSavePassword;
	public static String EditorSectionDomainNameLabel;
	public static String EditorSectionAppNameLabel;
	public static String EditorSectionDeployLocLabel;
	public static String EditorSectionOutputDestLabel;
	public static String EditorSectionConnectionLabel;
	public static String EditorSectionRemoteLabel;
	public static String EditorSectionBrowseDestButton;
	public static String EditorSectionProjectSettingsGroup;
	public static String EditorSectionOverrideProjectSettings;
	public static String PublishDialogCustomizeGitCommitMsg;
	public static String PublishDialogDefaultGitCommitMsg;
	public static String ClientReadTimeout;
	public static String TerminateConsole;

}
