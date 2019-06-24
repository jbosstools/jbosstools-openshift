/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.core.IDialogProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

public class DialogProvider implements IDialogProvider {

	@Override
	public void warn(String title, String message, String preferencesKey) {
		IPreferenceStore preferences = OpenShiftUIActivator.getDefault().getPreferenceStore();
		String prefsValue = preferences.getString(preferencesKey);
		boolean warn = !MessageDialogWithToggle.ALWAYS.equals(prefsValue);
		if (warn) {
			Display.getDefault().syncExec(() ->
				MessageDialogWithToggle.openWarning(
						Display.getDefault().getActiveShell(),
						title,
						message,
						"Dont remind me again",
						!warn,
						preferences,
						preferencesKey)
			);
		}
	}
}
