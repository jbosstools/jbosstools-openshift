/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jsch.internal.core.IConstants;
import org.eclipse.jsch.internal.core.JSchCorePlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;

/**
 * @author Andre Dietisheim
 */
public class SSHUtils {

	private static final String SSH_PREFERENCE_PAGE_ID = "org.eclipse.jsch.ui.SSHPreferences";
	private static final String KEYS_SEPARATOR = ",";

	public static String getSSH2Home() {
		return JSchCorePlugin.getPlugin().getPluginPreferences().getString(IConstants.KEY_SSH2HOME);
	}

	public static void setPrivateKeyPermissions(File privateKey) {
		// set f permission to correspond to 'chmod 0600' read/write only for
		// user
		// First clear all permissions for both user and others
		privateKey.setReadable(false, false);
		privateKey.setWritable(false, false);
		// Enable only readable for user
		privateKey.setReadable(true, true);
		privateKey.setWritable(true, true);
	}

	public static String[] getPrivateKeysFromPreferences() {
		String privateKeys =
				JSchCorePlugin.getPlugin().getPluginPreferences().getString(IConstants.KEY_PRIVATEKEY);
		if (privateKeys != null
				&& privateKeys.trim().length() > 0) {
			return privateKeys.split(KEYS_SEPARATOR);
		} else {
			return new String[0];
		}
	}

	public static boolean isPrivateKeyForPublicKeyKnownToSsh(String publicKeyPath) {
		if (StringUtils.isEmpty(publicKeyPath)) {
			return false;
		}
		for (String preferencesKey : getPrivateKeysFromPreferences()) {
			try {
				File privateKey = SshPrivateKeysPreferences.getKeyFile(preferencesKey);
				if (privateKey == null
						|| !FileUtils.canRead(privateKey)) {
					continue;
				}
				if (publicKeyPath.startsWith(privateKey.getAbsolutePath() + ".")
						|| publicKeyPath.startsWith(privateKey.getPath() + ".")) {
					return true;
				}
			} catch (FileNotFoundException e) {
				continue;
			}
		}
		return false;
	}

	public static void addToPrivateKeysPreferences(File privateKey) {
		Preferences preferences = JSchCorePlugin.getPlugin().getPluginPreferences();
		String privateKeys = preferences.getString(IConstants.KEY_PRIVATEKEY);
		String privateKeyPath = getKeyPath(privateKey);
		
		if (privateKeys != null
				&& privateKeys.trim().length() > 0) {
			privateKeys = privateKeys + "," + privateKeyPath;
		} else {
			privateKeys = privateKeyPath;
		}
		preferences.setValue(IConstants.KEY_PRIVATEKEY, privateKeys);
		JSchCorePlugin.getPlugin().setNeedToLoadKeys(true);
		JSchCorePlugin.getPlugin().savePluginPreferences();
	}

	private static String getKeyPath(File privateKey) {
		String ssh2Home = getSSH2Home();
		if (ssh2Home == null
				|| ssh2Home.isEmpty()) {
			return privateKey.getAbsolutePath();
		}
		
		if (!privateKey.getAbsolutePath().startsWith(ssh2Home)) {
			return privateKey.getAbsolutePath(); 
		}
		
		return privateKey.getName();
	}

	public static int openPreferencesPage(Shell shell) {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
				shell, SSH_PREFERENCE_PAGE_ID, null, null);
		return dialog.open();
	}

	/**
	 * Returns <code>true</code> if the given 
	 * @param publicKeyPath
	 * @return
	 */
	public static boolean publicKeyMatchesPrivateKeyInPreferences(File publicKey) {
		for (String preferencesKey : SSHUtils.getPrivateKeysFromPreferences()) {
			try {
				File privateKey = SshPrivateKeysPreferences.getKeyFile(preferencesKey.trim());
				if (privateKey == null) {
					continue;
				}
				if (publicKey.getAbsolutePath().startsWith(privateKey.getAbsolutePath() + ".")) {
					return true;
				}
			} catch (FileNotFoundException e) {
				continue;
			}
		}
		return false;
	}

	/**
	 * Returns the key file for a given relative or absolute keyPath. The
	 * keyPath may be absolute or relative to the ssh home directory.
	 * 
	 * @param keyPath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getKeyForRelativeOrAbsolutePath(String keyPath) throws FileNotFoundException {
		if (isEmpty(keyPath)) {
			return null;
		}

		if (keyPath.startsWith(File.separator)) {
			return new File(keyPath);
		} else {
			return new File(getSSH2Home(), keyPath);
		}
	}

	private static boolean isEmpty(String string) {
		return string == null
				|| string.isEmpty();
	}

}
