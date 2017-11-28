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
package org.jboss.tools.openshift.ui.bot.test.integration.openshift.io;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.equinox.security.ui.storage.StoragePreferencePage;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.impl.browser.InternalBrowser;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.swt.impl.tree.DefaultTreeItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShifIOPreferencePage;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.SecureStorage;
import org.jboss.tools.openshift.reddeer.utils.SystemProperties;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
public class GetOpenShiftIOTokenTest {

	@Test
	public void testGetToken() {
		new DefaultToolItem(new WorkbenchShell(), "Connect to OpenShift.io").click();
		DefaultShell browser = new DefaultShell();
		InternalBrowser internalBrowser = new InternalBrowser(browser);
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		new WaitUntil(new BrowserContainsText("OpenShift.io Developer Preview"), TimePeriod.LONG);
		
		internalBrowser.execute(String.format("document.getElementById(\"username\").value=\"%s\"", DatastoreOS3.OPENSHIFT_IO_USERNAME));
		internalBrowser.execute(String.format("document.getElementById(\"password\").value=\"%s\"", DatastoreOS3.OPENSHIFT_IO_PASSWORD));
		internalBrowser.execute("document.getElementById(\"password\").parentElement.parentElement.parentElement.submit()");
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		
		try {
			new DefaultShell("OpenShift.io");
		} catch (CoreLayerException ex) {
			//Secure storage has been triggered
			SecureStorage.handleSecureStoragePasswordAndHint(SystemProperties.SECURE_STORAGE_PASSWORD);
			new DefaultShell("OpenShift.io");
		}
		new OkButton().click();
		
		checkAccountInProperties();
		checkPluginInSecureStorage();

	}
	
	private void checkPluginInSecureStorage() {
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		StoragePreferencePage storagePreferencePage = new StoragePreferencePage(preferences);
		preferences.select(storagePreferencePage);
		storagePreferencePage.selectContentTab();
		DefaultTreeItem account = new DefaultTreeItem(new DefaultTree(storagePreferencePage, 1), "[Default Secure Storage]","org.jboss.tools.openshift.io.core", "accounts", "OpenShift.io", DatastoreOS3.OPENSHIFT_IO_USERNAME);
		account.select();
		assertNotNull("Account does not exists in secure storage", account);
		preferences.ok();
	}

	private void checkAccountInProperties() {
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		OpenShifIOPreferencePage page = new OpenShifIOPreferencePage(preferences);
		preferences.select(page);
		assertTrue("Account is not configured!",page.existsOpenShiftIOAccount());
		preferences.ok();
	}
	
	@AfterClass
	public static void cleanUp() {
		removeAccountFromOpenShiftIOPreferencePage();
	}
	
	private static void removeAccountFromOpenShiftIOPreferencePage() {
		WorkbenchPreferenceDialog preferences = new WorkbenchPreferenceDialog();
		preferences.open();
		OpenShifIOPreferencePage page = new OpenShifIOPreferencePage(preferences);
		preferences.select(page);
		if (page.existsOpenShiftIOAccount()) {
			page.remove();
		}
		preferences.ok();
	}
	
}
