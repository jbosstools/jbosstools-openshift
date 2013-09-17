/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.openshift.express.internal.core.util.FileUtils;

import com.openshift.client.OpenShiftException;

/**
 * A class that parses the user ssh configuration and retrieves the
 * <tt>IdentityFile</tt> setting from a <tt>Host</tt> block that is configuring
 * the OpenShift application (<tt>rhcloud.com</tt>) PaaS.
 * 
 * @author Andre Dietisheim
 */
public class SSHUserConfig {

	private static final String VALUE_LIBRA_SSH_HOST = "rhcloud.com";
	private static final String KEY_CONFIGBLOCK_HOST_START = "Host ";
	private static final String KEY_CONFIGBLOCK_HOST_END = "\n";
	private static final Pattern IDENTITYFILE_PATTERN = Pattern.compile(".+IdentityFile (.+)");
	private static final String CONFIG_FILENAME = "config";
	private File configFile;

	public SSHUserConfig(String sshHome) {
		this(new File(sshHome, CONFIG_FILENAME));
	}

	public SSHUserConfig(File configFile) {
		this.configFile = configFile;
	}

	public boolean exists() {
		return FileUtils.canRead(configFile);
	}

	public File getFile() {
		return configFile;
	}
	
	public boolean hasLibraIdentifyFile() throws OpenShiftException {
		return getLibraIdentityFile() != null;
	}

	public String getLibraIdentityFile() throws OpenShiftException {
		if (!exists()) {
			return null;
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(configFile));
			for (String data = reader.readLine(); data != null; data = reader.readLine()) {
				if (!data.startsWith(KEY_CONFIGBLOCK_HOST_START)
						|| !data.endsWith(VALUE_LIBRA_SSH_HOST)) {
					continue;
				}

				for (data = reader.readLine(); data != null; reader.readLine()) {
					if (data.equals(KEY_CONFIGBLOCK_HOST_END)) {
						continue;
					}

					Matcher matcher = IDENTITYFILE_PATTERN.matcher(data);
					if (!matcher.find()
							|| matcher.groupCount() < 1) {
						continue;
					}

					return matcher.group(1);
				}
			}
			return null;
		} catch (IOException e) {
			throw new OpenShiftException("Could not read file {0}", configFile);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				// ignore THIS IS INTENTIONAL
			}
		}
	}
}
