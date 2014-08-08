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
	public static String DomainName;
	public static String EnterDomainName;
	public static String DomainNameMayHaveLettersAndDigits;
	public static String DomainNameMaximumLength;
	public static String DeletingOpenShiftApplications;
	public static String DeletingApplication;
	public static String FailedToDeleteApplication;
	
	public static String EnvironmentVariables;
	public static String PleaseProvadeNewVariable;
	public static String Add;
	public static String Edit;
	public static String Remove;
	public static String Refresh;
	public static String ServerDoesNotSupportChanging;
	public static String CouldNotLoadVariables;
	public static String RemoveVariable;
	public static String DoYouWantToRemoveVariable;
	public static String CouldNotRefreshVariables;
	public static String LoadingVariables;
	public static String RefreshVariables;
	public static String DoYouWantToRefreshVariables;
	public static String RefreshingVariables;

	public static String NoConnectionsAreAvailable;
	
	public static String OpenShiftServerWizardConnection;
	public static String OpenShiftServerWizardNew;
	public static String OpenShiftServerWizardDomainName;
	public static String OpenShiftServerWizardApplicationName;
	public static String OpenShiftServerWizardDeployProject;
	public static String OpenShiftServerWizardImportLink;
	public static String OpenShiftServerWizardCreateLink;
	public static String OpenShiftServerWizardRemote;
	public static String OpenShiftServerWizardProjectSettings;
	public static String OpenShiftServerWizardOutputDirectory;
	public static String OpenShiftServerWizardBrowse;
	public static String OpenShiftServerWizardCouldNotGetRemotePointing;
	public static String OpenShiftServerWizardDeployLocation;
	public static String OpenShiftServerWizardPleaseChooseLocation;
	public static String OpenShiftServerWizardPleaseSelectConnection;
	public static String OpenShiftServerWizardPleaseCreateDomain;
	public static String OpenShiftServerWizardPleaseSelectDomain;
	public static String OpenShiftServerWizardPleaseSelectApplication;
	public static String OpenShiftServerWizardPleaseCreateApplication;
	public static String OpenShiftServerWizardYourWorkspaceDoesNotHaveProject;
	public static String OpenShiftServerWizardCouldNotLoadDomains;
	public static String OpenShiftServerWizardFetchingDomainsAndApplications;

}
