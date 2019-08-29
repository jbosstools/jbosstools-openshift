/*******************************************************************************
 * Copyright (c) 2016-2019 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server.behavior;

import java.util.LinkedHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.jboss.tools.openshift.core.IDialogProvider;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCorePreferences;
import org.jboss.tools.openshift.internal.core.Dialogs;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.osgi.service.prefs.BackingStoreException;

public class JavaDebugPreferences {

	private static final String JAVA_DEBUG_PREFERENCE_PAGE_ID = "org.eclipse.jdt.debug.ui.JavaDebugPreferencePage";

	public void warnIfShowMethodResultEnabled(String mode) throws CoreException {
		if (ILaunchManager.DEBUG_MODE.equalsIgnoreCase(mode)
				&& isShowMethodResultEnabled()
				&& warnIfShowMethodResult()) {
				enableShowMethodResult(false);
		}
	}

	private boolean warnIfShowMethodResult() {
		LinkedHashMap<String, Integer> buttonLabelToIdMap = new LinkedHashMap<>();
		buttonLabelToIdMap.put("Disable", IDialogProvider.YES_ID);
		buttonLabelToIdMap.put("Don't disable", IDialogProvider.NO_ID);

		int answer = Dialogs.INSTANCE.message("Very slow stepping",
				IDialogProvider.WARNING,
				"Java Debug preference <a>Show method result after a step operation</a> is turned on.\n"
				+ "This results in very poor performance when stepping.\n\n"
				+ "Disable 'Show method result'?",
				linkText -> Dialogs.INSTANCE.preferencePage(JAVA_DEBUG_PREFERENCE_PAGE_ID),
				buttonLabelToIdMap,
				0,
				IOpenShiftCorePreferences.WARN_SHOW_METHOD_RESULT);
		return IDialogProvider.YES_ID == answer;
	}

	public boolean isShowMethodResultEnabled() {
		return Platform.getPreferencesService().getBoolean(
				JDIDebugPlugin.getUniqueIdentifier(), JDIDebugModel.PREF_SHOW_STEP_RESULT, true, null);
	}

	public void enableShowMethodResult(boolean enable) throws CoreException {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(JDIDebugPlugin.getUniqueIdentifier());
		prefs.put(JDIDebugModel.PREF_SHOW_STEP_RESULT, String.valueOf(enable));
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			throw new CoreException(OpenShiftCoreActivator.statusFactory().errorStatus(e));
		}
	}

}
