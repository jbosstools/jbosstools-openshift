/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.preferences;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final String DOWNLOAD_INSTRUCTIONS_URL = 
			"https://github.com/openshift/origin/blob/master/CONTRIBUTING.adoc#download-from-github";
	
	private CliFileEditor cliLocationEditor;
	private OCBinary ocBinary;
	
	public OpenShiftPreferencePage() {
		super(GRID);
		this.ocBinary = OCBinary.getInstance();
	}
	
	public void createFieldEditors() {
		Link link = new Link(getFieldEditorParent(), SWT.WRAP);
		link.setText("The OpenShift Client binary (oc) is required for features such as Port Forwarding or Log Streaming. "
				+ "You can find more information about how to install it from <a>here</a>.");
		GridDataFactory.fillDefaults().span(3, 1).hint(300, SWT.DEFAULT).applyTo(link);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new BrowserUtility().checkedCreateExternalBrowser(DOWNLOAD_INSTRUCTIONS_URL, 
																  OpenShiftUIActivator.PLUGIN_ID, 
																  OpenShiftUIActivator.getDefault().getLog());
			}
		});
		this.cliLocationEditor = new CliFileEditor();
		cliLocationEditor.setFilterPath(SystemUtils.getUserHome());
		cliLocationEditor.setFileExtensions(ocBinary.getExtensions());
		cliLocationEditor.setValidateStrategy(FileFieldEditor.VALIDATE_ON_KEY_STROKE);
		addField(cliLocationEditor);
    }
	
	public void init(IWorkbench workbench) {
		setPreferenceStore(OpenShiftUIActivator.getDefault().getCorePreferenceStore());
	}

	@Override
	protected void performDefaults() {
		String location = ocBinary.getSystemPathLocation();
		if(location == null) {
			//We have to update default value in preferences even if it is empty.
			location = "";
		}
		getPreferenceStore().setDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC, location);

		if(StringUtils.isBlank(location)) {
			String message = NLS.bind("Could not find the OpenShift Client executable \"{0}\" on your path.", ocBinary.getName());
			OpenShiftUIActivator.getDefault().getLogger().logWarning(message);				
			MessageDialog.openWarning(getShell(), "No OpenShift Client executable", message);
			return;
		}

		super.performDefaults();

		//Super implementation changes instance value, we need it clean.
		getPreferenceStore().setToDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);

	}

	@Override
	public boolean performOk() {
		boolean valid = true;
		if(cliLocationEditor.getStringValue().equals(getPreferenceStore().getDefaultString(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC))) {
			//Super implementation changes instance value, we need it clean.
			getPreferenceStore().setToDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);
		} else {
			valid = super.performOk();
		}
		valid = validateLocation(cliLocationEditor.getStringValue()) && valid;
		setValid(valid);
		return valid;
	}

	private boolean validateLocation(String location) {
		if(StringUtils.isBlank(location)) {
			return true;
		}
		File file = new File(location);
		//Error messages have to be set to field editor, not directly to the page.
		if(!ocBinary.getName().equals(file.getName())) {
			cliLocationEditor.setErrorMessage(NLS.bind("{0} is not the OpenShift Client ''{1}'' executable.", file.getName(), ocBinary.getName()));
			return false;
		}
		if(!file.exists()) {
			cliLocationEditor.setErrorMessage(NLS.bind("{0} was not found.", file));
			return false;
		}
		if(!file.canExecute()) {
			cliLocationEditor.setErrorMessage(NLS.bind("{0} does not have execute permissions.", file));
			return false;
		}
		return true;
	}

	class CliFileEditor extends FileFieldEditor {
		public CliFileEditor() {
			//Validation strategy should be set in constructor, later setting it has no effect.
			super(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC,
					NLS.bind("''{0}'' executable location", ocBinary.getName()), false, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		}

		@Override
		protected boolean checkState() {
			//We have to return the default error message that is used 
			//by super implementation if file does not exist. 
			setErrorMessage(JFaceResources.getString("FileFieldEditor.errorMessage"));
			return super.checkState();
		}

		@Override
		public boolean doCheckState() {
			return validateLocation(getStringValue());
		}

		@Override
		protected void valueChanged() {
			super.valueChanged();
		}
	}
}