/******************************************************************************* 
 * Copyright (c) 2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.crc.server.ui;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PullSecretValidation {

	private static final int MAX_FILE_SIZE = 20 * 1024; // 20k

	private final AtomicReference<String> error = new AtomicReference<>();
	private final AtomicReference<String> file = new AtomicReference<>();

	public String validate(final String filename, final Consumer<String> listener) {
		if (!hasChanged(filename)) {
			return error.get();
		}
		error.set(null);
		this.file.set(filename); 
		new ValidationJob(filename, listener).schedule();
		return error.get();
	}

	private boolean hasChanged(String filename) {
		return !Objects.equals(file.get(), filename);
	}

	private class ValidationJob extends UIUpdatingJob {

		private String pullSecretFile;
		private Consumer<String> listener;
		
		private ValidationJob(String pullSecretFile, Consumer<String> listener) {
			super("Validating pull secret...");
			this.pullSecretFile = pullSecretFile;
			this.listener = listener;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String errorMessage = validate(pullSecretFile);
			error.set(errorMessage);
			return Status.OK_STATUS;
		}

		@Override
		protected IStatus updateUI(IProgressMonitor monitor) {
			listener.accept(error.get());
			return Status.OK_STATUS;
		}

		private String validate(String filename) {
			String msg = null;
			if (filename == null || filename.isEmpty() || !(new File(filename)).isFile()) {
				msg = "Please select a valid Pull Secret file. See https://cloud.redhat.com/openshift/install/crc/installer-provisioned for instructions.";
			} else if (new File(filename).length() > MAX_FILE_SIZE) {
				msg = "Please select a Pull Secret file that's < 20KB. See https://cloud.redhat.com/openshift/install/crc/installer-provisioned for instructions.";
			} else if (!new File(filename).canRead()) {
				msg = "Pull Secret file is not readable.";
			} else {
				msg = validateJSON(filename);
			}
			return msg;
		}

		private String validateJSON(String filename) {
			String msg = null;
			try {
				if (!isValidJson(readPullSecretFile(filename))) {
					msg = NLS.bind("Pull Secret file {0} is invalid. Make sure it is valid JSON.", filename);
				}
			} catch (IOException | UncheckedIOException e) {
				msg = NLS.bind("Pull Secret file {0} is invalid. Make sure it is valid JSON: {1}", filename, e.getLocalizedMessage());
			}
			return msg;
		}

		private String readPullSecretFile(String pullSecretFile) throws IOException {
			String content = null;
			try (Stream<String> lines = Files.lines(Paths.get(pullSecretFile))) {
				content = lines
						.limit(MAX_FILE_SIZE)
						.collect(Collectors.joining());
			}
			return content;
		}

		private boolean isValidJson(String json) {
			return isJSONObject(json)
					|| isJSONArray(json);
		}

		private boolean isJSONObject(String json) {
			try {
				new JSONObject(json);
				return true;
			} catch (JSONException ex) {
				return false;
			}		
		}

		private boolean isJSONArray(String json) {
			try {
				new JSONArray(json);
				return true;
			} catch (JSONException ex) {
				return false;
			}		
		}
	}
}