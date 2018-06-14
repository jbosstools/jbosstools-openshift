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
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import java.util.List;

import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.equinox.security.ui.storage.PasswordProvider;
import org.eclipse.reddeer.eclipse.equinox.security.ui.storage.StoragePreferencePage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.BackButton;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.NextButton;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.CentralIsLoaded;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.SecureStorage;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@OpenPerspective(value=JBossPerspective.class)
@SuppressWarnings("restriction")
@OCBinary(setOCInPrefs=true, cleanup=false)
@RequiredBasicConnection
@RequiredProject
@RunWith(RedDeerSuite.class)
public class StoreConnectionTest extends AbstractTest {
	
	@BeforeClass
	public static void setupClass(){
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		StoragePreferencePage storagePreferencePage = new StoragePreferencePage(preferences);
		preferences.select(storagePreferencePage);
		storagePreferencePage.selectPasswordsTab();
		List<PasswordProvider> masterPasswordProviders = storagePreferencePage.getMasterPasswordProviders();
		for (PasswordProvider tableItem : masterPasswordProviders) {
			// The second part of this if is because https://issues.jboss.org/browse/JBIDE-24567
			if (tableItem.getDescription().contains("UI Prompt") ||tableItem.getDescription().contains("secureStorageProvider.name")){
				tableItem.setEnabled(true);
			}else{
				tableItem.setEnabled(false);
			}
		}
		preferences.ok();
	}
	
	@Test
	public void secureStorageDisabledJBIDE19604Test() {
		deleteSecureStorage();
		
		invokeNewAppWizardFromCentral();
		
		new CheckBox(OpenShiftLabel.TextLabels.STORE_PASSWORD).toggle(true);
		new NextButton().click();
		
		//Cancel secure storage shell
		try {
			new DefaultShell(OpenShiftLabel.Shell.SECURE_STORAGE_PASSWORD);
		} catch (CoreLayerException ex) {
			new DefaultShell(OpenShiftLabel.Shell.SECURE_STORAGE);
		}
		new CancelButton().click();
		
		//Cancel warning shell
		new DefaultShell("Warning");
		new OkButton().click();
		new CheckBox(OpenShiftLabel.TextLabels.STORE_PASSWORD).toggle(false);
		
		//Next button should work
		new NextButton().click();
		AbstractWaitCondition backButtonIsEnabled = new AbstractWaitCondition() {
			
			@Override
			public boolean test() {
				return new BackButton().isEnabled();
			}
		};
		new WaitUntil(backButtonIsEnabled, TimePeriod.LONG);
		
		new CancelButton().click();
	}

	private void invokeNewAppWizardFromCentral() {
		new DefaultToolItem(new WorkbenchShell(), OpenShiftLabel.Others.RED_HAT_CENTRAL).click();
		
		new WaitUntil(new CentralIsLoaded());
		
		new InternalBrowser().execute(OpenShiftLabel.Others.OPENSHIFT_CENTRAL_SCRIPT);
	
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
				TimePeriod.LONG);
		new DefaultShell("New OpenShift Application");
	}
	
	private void deleteSecureStorage() {
		ISecurePreferences defaultStorage = SecurePreferencesFactory.getDefault();
		defaultStorage.clear();
		defaultStorage.removeNode();

		// clear it from the list of open storages, delete the file 
		InternalExchangeUtils.defaultStorageDelete();
	}

	@Test
	public void shouldStoreAndRemovePassword() {
		SecureStorage.storeOpenShiftPassword(DatastoreOS3.USERNAME, DatastoreOS3.SERVER);
		SecureStorage.verifySecureStorageOfPassword(
				DatastoreOS3.USERNAME, DatastoreOS3.SERVER.substring(8), true);

		SecureStorage.removeOpenShiftPassword(DatastoreOS3.USERNAME, DatastoreOS3.SERVER);
		SecureStorage.verifySecureStorageOfPassword(
				DatastoreOS3.USERNAME, DatastoreOS3.SERVER.substring(8), false);
	}
	
	@AfterClass
	public static void cleanUp(){
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		StoragePreferencePage storagePreferencePage = new StoragePreferencePage(preferences);
		preferences.select(storagePreferencePage);
		storagePreferencePage.selectPasswordsTab();
		List<PasswordProvider> masterPasswordProviders = storagePreferencePage.getMasterPasswordProviders();
		for (PasswordProvider tableItem : masterPasswordProviders) {
			tableItem.setEnabled(true);
		}
		preferences.ok();
	}
}
