/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
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
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.equinox.security.ui.StoragePreferencePage;
import org.jboss.reddeer.swt.api.TableItem;
import org.jboss.reddeer.swt.impl.browser.InternalBrowser;
import org.jboss.reddeer.swt.impl.button.BackButton;
import org.jboss.reddeer.swt.impl.button.CancelButton;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.NextButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.jboss.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.condition.CentralIsLoaded;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.SecureStorage;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("restriction")
@RequiredBasicConnection()
@RequiredProject
public class StoreConnectionTest {
	
	@BeforeClass
	public static void setupClass(){
		StoragePreferencePage storagePreferencePage = new StoragePreferencePage();
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		preferences.select(storagePreferencePage);
		List<TableItem> masterPasswordProviders = storagePreferencePage.getMasterPasswordProviders();
		for (TableItem tableItem : masterPasswordProviders) {
			// The second part of this if is because https://issues.jboss.org/browse/JBIDE-24567
			if (tableItem.getText().contains("UI Prompt") ||tableItem.getText().contains("secureStorageProvider.name")){
				tableItem.setChecked(true);
			}else{
				tableItem.setChecked(false);
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
		new DefaultShell("Secure Storage Password");
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
		new WaitUntil(backButtonIsEnabled);
		
		new CancelButton().click();
	}

	private void invokeNewAppWizardFromCentral() {
		new DefaultToolItem(new WorkbenchShell(), OpenShiftLabel.Others.RED_HAT_CENTRAL).click();
		
		new WaitUntil(new CentralIsLoaded());
		
		new InternalBrowser().execute(OpenShiftLabel.Others.OPENSHIFT_CENTRAL_SCRIPT);
	
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.NEW_APP_WIZARD),
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
}
