/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author Andre Dietisheim
 */
public class ServerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ServerPreferencePage() {
		super(GRID);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		createServerControls((Composite) control);
		return control;
	}

	private void createServerControls(Composite parent) {
		Group defaultServerGroup = new Group(parent, SWT.NONE);
		defaultServerGroup.setText("Default Server");
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.FILL, SWT.FILL).applyTo(defaultServerGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).applyTo(defaultServerGroup);
	}

	public void createFieldEditors() {
		addField(new StringFieldEditor(
				OpenShiftPreferences.DEFAULT_HOST
				, OpenShiftPreferences.INSTANCE.getDefaultHost()
				, getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
		setPreferenceStore(OpenShiftPreferences.INSTANCE.getPreferencesStore());
		setDescription("Server");
	}

	@Override
	public boolean performOk() {
		OpenShiftPreferences.INSTANCE.flush();
		return super.performOk();
	}
}