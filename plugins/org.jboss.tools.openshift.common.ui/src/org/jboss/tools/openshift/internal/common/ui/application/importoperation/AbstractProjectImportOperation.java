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
package org.jboss.tools.openshift.internal.common.ui.application.importoperation;

import java.io.File;

/**
 * @author André Dietisheim <adietish@redhat.com>
 */
public class AbstractProjectImportOperation {

	private File projectFolder;

	public AbstractProjectImportOperation(File projectDirectory) {
		this.projectFolder = projectDirectory;
	}

	protected File getProjectDirectory() {
		return projectFolder;
	}

	protected boolean isReadable(File destination) {
		return destination != null
				&& destination.exists()
				&& destination.canRead();
	}
}