/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.crc.server.ui.detection;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.internal.crc.server.core.adapter.CRC100Server;
import org.jboss.tools.runtime.core.model.IRuntimeDetectionResolution;
import org.jboss.tools.runtime.core.model.IRuntimeDetectionResolutionProvider;
import org.jboss.tools.runtime.core.model.RuntimeDefinition;
import org.jboss.tools.runtime.core.model.RuntimeDetectionProblem;

public class MissingPullSecretResolutionProvider implements IRuntimeDetectionResolutionProvider {
	public static final int PROBLEM_CRC_MISSING_PULL_SECRET = 2020;

	public MissingPullSecretResolutionProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IRuntimeDetectionResolution[] getResolutions(RuntimeDetectionProblem problem, RuntimeDefinition def) {
		if (problem.getCode() == PROBLEM_CRC_MISSING_PULL_SECRET) {
			return new IRuntimeDetectionResolution[] { new MissingPullSecretResolution() };
		}
		return null;
	}
	public static class MissingPullSecretResolution implements IRuntimeDetectionResolution {

		@Override
		public String getLabel() {
			return "Set CRC Pull Secret file.";
		}

		@Override
		public void run(RuntimeDetectionProblem problem, RuntimeDefinition definition) {
			File found = getFile(null, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			if (found != null) {
				definition.setProperty(CRC100Server.PROPERTY_PULL_SECRET_FILE,
						found.getAbsolutePath());
			}
		}

		protected static File getFile(File startingDirectory, Shell shell) {
			FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
			if (startingDirectory != null) {
				fileDialog.setFilterPath(startingDirectory.getPath());
			}

			String dir = fileDialog.open();
			if (dir != null) {
				dir = dir.trim();
				if (dir.length() > 0) {
					return new File(dir);
				}
			}
			return null;
		}
	}

}
