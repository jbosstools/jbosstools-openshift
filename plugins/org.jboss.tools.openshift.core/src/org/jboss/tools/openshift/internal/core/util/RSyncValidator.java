/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

import org.jboss.tools.openshift.core.OpenShiftCoreMessages;

/**
 * Utility to validate rsync environment. On Linux, check if rsync is on the
 * PATH. On Windows, check if rsync is on the PATH and if the fstab file has the
 * noacl bit set.
 */
public class RSyncValidator {

	public enum RsyncStatus {
		RSYNC_NOT_FOUND(OpenShiftCoreMessages.RsyncNotFoundMessage), ETC_FSTAB_NOT_FOUND(
				OpenShiftCoreMessages.RsyncFstabNotFoundMessage,
				OpenShiftCoreMessages.RsyncFstabNotFoundDetailedMessage), ETC_FSTAB_INVALID(
						OpenShiftCoreMessages.RsyncFstabInvalidMessage,
						OpenShiftCoreMessages.RsyncFstabInvalidDetailedMessage), OK("");

		private final String message;

		private final String detailedMessage;

		private RsyncStatus(String message, String detailedMessage) {
			this.message = message;
			this.detailedMessage = detailedMessage;
		}

		private RsyncStatus(String message) {
			this(message, message);
		}

		public String getMessage() {
			return message;
		}

		public String getDetailedMessage(String base) {
			return NLS.bind(detailedMessage, base);
		}
	}

	private static RSyncValidator INSTANCE;

	public static RSyncValidator get() {
		if (INSTANCE == null) {
			INSTANCE = new RSyncValidator();
		}
		return INSTANCE;

	}

	private RsyncStatus status = RsyncStatus.OK;

	private String rsyncPath;

	private static final String ETC_FSTAB_LOCATION = "etc/fstab";

	private RSyncValidator() {
		try {
			rsyncPath = getRSyncPath();
			refresh();
		} catch (IOException e) {
			status = RsyncStatus.RSYNC_NOT_FOUND;
		}
	}

	public void refresh() {
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			checkFstab();
		}
	}

	public RsyncStatus getStatus() {
		return status;
	}

	public String getBasePath() {
		return (rsyncPath != null) ? Paths.get(rsyncPath).getParent().getParent().toString() : "";
	}

	private String getRSyncPath() throws IOException {
		ProcessBuilder builder;
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			builder = new ProcessBuilder("where", "rsync.exe");
		} else {
			builder = new ProcessBuilder("which", "rsync");
		}
		Process process = builder.start();
		String path;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			path = reader.readLine();
		}
		try {
			if (process.waitFor() == 0) {
				return path;
			} else {
				throw new IOException();
			}
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	private void checkFstab() {
		File f = new File(getBasePath(), ETC_FSTAB_LOCATION);
		if (f.exists()) {
			String line = null;
			status = RsyncStatus.ETC_FSTAB_INVALID;
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#")) {
						if (line.contains("cygdrive") && line.contains("noacl")) {
							status = RsyncStatus.OK;
						}
					}
				}
			} catch (IOException e) {
				status = RsyncStatus.ETC_FSTAB_INVALID;
			}

		} else {
			status = RsyncStatus.ETC_FSTAB_NOT_FOUND;
		}
	}
}
