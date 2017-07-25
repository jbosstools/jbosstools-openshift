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
package org.jboss.tools.openshift.reddeer.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.eclipse.equinox.security.ui.StoragePreferencePage;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.exception.SWTLayerException;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.FinishButton;
import org.jboss.reddeer.swt.impl.button.NoButton;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.reddeer.workbench.impl.shell.WorkbenchShell;
import org.jboss.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.common.reddeer.label.IDELabel.ServerType;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.AbstractOpenShiftConnection;

public class SecureStorage {

	private static final Logger LOGGER = new Logger(SecureStorage.class);

	private SecureStorage() {
		throw new IllegalAccessError("Utility class");
	}

	/**
	 * Store password in secure storage for specified user.
	 * 
	 * @param username
	 */
	public static void storeOpenShiftPassword(String username, String server) {
		triggerSecureStorageOfPasswordInConnectionDialog(username, server, true);
	}

	/**
	 * Remove password in secure storage for specified user.
	 * 
	 * @param username
	 */
	public static void removeOpenShiftPassword(String username, String server) {
		triggerSecureStorageOfPasswordInConnectionDialog(username, server, false);
	}

	/**
	 * Triggers secure storage of password.
	 * 
	 * @param username
	 *            user name
	 * @param server
	 *            server
	 * @param storePassword
	 *            store password if value is set to true, remove password if value
	 *            is set to false
	 * @param serverType
	 *            type of a server
	 */
	private static void triggerSecureStorageOfPasswordInConnectionDialog(String username, String server,
			boolean storePassword) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		AbstractOpenShiftConnection connection = explorer.getOpenShiftConnection(username, server);
		connection.select();

		new ContextMenu(OpenShiftLabel.ContextMenu.EDIT_CONNECTION).select();

		new DefaultShell(OpenShiftLabel.Shell.EDIT_CONNECTION);

		// Store password if it is not stored
		if (!(new CheckBox(1).isChecked()) && storePassword) {
			new CheckBox(1).click();
			new FinishButton().click();

			TestUtils.acceptSSLCertificate();

			boolean firstStorage = provideSecureStoragePassword(SystemProperties.SECURE_STORAGE_PASSWORD);

			if (firstStorage) {
				// Did "Password hint needed" shell appear? Get rid of it.
				try {
					new DefaultShell(OpenShiftLabel.Shell.PASSWORD_HINT_NEEDED);
					new NoButton().click();
				} catch (SWTLayerException ex) {
					// do nothing
					LOGGER.debug("Password hint did not appear. Skipping.");
				}
			}
			// Remove password if it is stored
		} else if (new CheckBox(1).isChecked() && !storePassword) {
			new CheckBox(1).click();
			new FinishButton().click();
		}

		new WaitWhile(new JobIsRunning());
	}

	private static boolean provideSecureStoragePassword(String password) {
		try {
			new DefaultShell(OpenShiftLabel.Shell.SECURE_STORAGE_PASSWORD);
		} catch (SWTLayerException ex) {
			LOGGER.info(String.format("Shell with label \"%s\" was not found. Trying \"%s\"",
					OpenShiftLabel.Shell.SECURE_STORAGE_PASSWORD, OpenShiftLabel.Shell.SECURE_STORAGE));
			new DefaultShell(OpenShiftLabel.Shell.SECURE_STORAGE);
		}
		new DefaultText(0).setText(password);
		boolean firstStorage = true;
		try {
			new DefaultText(1).setText(password);
		} catch (RedDeerException ex) {
			firstStorage = false;
		}

		new WaitUntil(new WidgetIsEnabled(new OkButton()));
		new OkButton().click();

		return firstStorage;
	}

	/**
	 * Verifies whether state of password storage for specified user is in correct
	 * state.
	 *
	 * @param username
	 * @param server
	 *            URL of OpenShift server without https prefix
	 * @param shouldExist
	 *            if true, password should be present in secure storage, if false,
	 *            there should be no password in secure storage for specified
	 *            connection
	 * @param serverType
	 *            OpenShift v2 or v3 server
	 */
	public static void verifySecureStorageOfPassword(String username, String server, boolean shouldExist) {
		WorkbenchPreferenceDialog workbenchPreferenceDialog = new WorkbenchPreferenceDialog();
		StoragePreferencePage secureStoragePreferencePage = new StoragePreferencePage();
		new WorkbenchShell().setFocus();
		workbenchPreferenceDialog.open();
		new WorkbenchPreferenceDialog().select(secureStoragePreferencePage);

		secureStoragePreferencePage.selectContentTab();
		boolean exists = secureStoragePreferencePage.passwordExists("[Default Secure Storage]", getPluginId(),
				server, username);

		workbenchPreferenceDialog.ok();

		assertTrue(shouldExist ? "Password wasn't present in secure storage while it should."
				: "Password was present in secure storage but it shouldn't.", shouldExist == exists);
	}

	private static String getPluginId() {
		String pluginId = "org.jboss.tools.openshift.core";
		return pluginId;
	}
}
