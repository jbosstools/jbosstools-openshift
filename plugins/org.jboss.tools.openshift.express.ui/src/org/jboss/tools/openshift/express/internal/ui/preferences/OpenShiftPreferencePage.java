/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.openshift.express.client.ClientSystemProperties;
import org.jboss.tools.openshift.express.internal.core.preferences.IExpressCoreConstants;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IntegerFieldEditor clientReadTimeout;

	public OpenShiftPreferencePage() {
		super(GRID);
	}

	public void createFieldEditors() {
		this.clientReadTimeout =
				new IntegerFieldEditor(
						IExpressCoreConstants.CLIENT_READ_TIMEOUT,
						ExpressUIMessages.ClientReadTimeout, getFieldEditorParent());
		clientReadTimeout.setValidRange(0, Integer.MAX_VALUE / 1000); // seconds
		addField(clientReadTimeout);
	}

	public void init(IWorkbench workbench) {
		IPreferenceStore preferenceStore = ExpressUIActivator.getDefault().getCorePreferenceStore();
		preferenceStore.setDefault(IExpressCoreConstants.CLIENT_READ_TIMEOUT, ClientSystemProperties.getReadTimeoutSeconds());
		setPreferenceStore(preferenceStore);
	}

	@Override
	public boolean performOk() {
		boolean returnValue = super.performOk();
		ClientSystemProperties.setReadTimeoutSeconds(clientReadTimeout.getIntValue());
		return returnValue;
	}
	
}