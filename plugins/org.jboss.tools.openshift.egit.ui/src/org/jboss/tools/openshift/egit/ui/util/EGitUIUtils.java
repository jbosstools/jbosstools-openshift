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
package org.jboss.tools.openshift.egit.ui.util;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;

public class EGitUIUtils {

	public static String getEGitDefaultRepositoryPath() {
		return Activator.getDefault().getPreferenceStore().getString(UIPreferences.DEFAULT_REPO_DIR);
	}

}
