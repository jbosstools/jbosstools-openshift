/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.core;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.ExtensionUtils;
import org.jboss.tools.openshift.core.IDialogProvider;

public class Dialogs implements IDialogProvider {

	private static final String EXTENSION = "org.jboss.tools.openshift.core.dialogProvider";
	private static final String ATTRIBUTE_CLASS = "class";

	public static final Dialogs INSTANCE = new Dialogs();

	private IDialogProvider provider;

	private Dialogs() {
		this.provider = get();
	}

	@Override
	public void warn(String title, String message, String preferencesKey) {
		if (provider == null) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not find extension ", EXTENSION));
			return;
		}
		provider.warn(title, message, preferencesKey);
	}
	
	@Override
	public int message(String title, int type, String message, Consumer<String> callback,
			LinkedHashMap<String, Integer> buttonLabelToIdMap, int defaultButton, String preferencesKey) {
		if (provider == null) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not find extension ", EXTENSION));
			return NO_ID;
		}
		return provider.message(title, type, message, callback, buttonLabelToIdMap, defaultButton, preferencesKey);
	}

	@Override
	public void preferencePage(String page) {
		if (provider == null) {
			OpenShiftCoreActivator.pluginLog().logError(NLS.bind("Could not find extension ", EXTENSION));
			return;
		}
		provider.preferencePage(page);
	}

	private IDialogProvider get() {
		return ExtensionUtils.getFirstExtension(EXTENSION, ATTRIBUTE_CLASS);
	}

}
