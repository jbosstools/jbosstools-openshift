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
package org.jboss.tools.openshift.express.internal.ui.common;

import java.io.File;

/**
 * @author André Dietisheim
 */
public class FileUtils {

	public static boolean canRead(String path) {
		if (path == null) {
			return false;
		}
		return canRead(new File(path));
	}

	public static boolean canRead(File file) {
		if (file == null) {
			return false;
		}
		return file.canRead();
	}

	
}
