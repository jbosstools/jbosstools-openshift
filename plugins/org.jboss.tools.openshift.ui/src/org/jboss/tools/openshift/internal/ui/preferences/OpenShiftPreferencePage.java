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
package org.jboss.tools.openshift.internal.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author jeff.cantrill
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final int SUCCESS = 0;
	private static final String OC_BINARY_NAME;
	private static final String [] EXTENSIONS;
	
	static {
		if(SystemUtils.IS_OS_WINDOWS) {
			EXTENSIONS = new String []{"exe"};
			OC_BINARY_NAME = "oc.exe";
		}else {
			EXTENSIONS = new String []{};
			OC_BINARY_NAME = "oc";
		}
	}
	private FileFieldEditor cliLocationEditor;
	
	public OpenShiftPreferencePage() {
		super(GRID);
	}
	
	public void createFieldEditors() {
		this.cliLocationEditor = 
				new FileFieldEditor(
						IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC, 
						NLS.bind("{0} location", OC_BINARY_NAME), getFieldEditorParent());
		this.cliLocationEditor.setFilterPath(SystemUtils.getUserHome());
		this.cliLocationEditor.setFileExtensions(EXTENSIONS);
		addField(cliLocationEditor);
		
	}
	
	public void init(IWorkbench workbench) {
		setPreferenceStore(OpenShiftUIActivator.getDefault().getCorePreferenceStore());
	}
	
	@Override
	protected void performDefaults() {
		String location = findOCLocation();
		if(StringUtils.isBlank(location)) {
			MessageDialog.openInformation(getShell(), "Unable to find OpenShift binary", "Unable to find the OpenShift binary in your path");
			return;
		}
		cliLocationEditor.setStringValue(location);
	}

	@Override
	public boolean performOk() {
		boolean valid = super.performOk() && validateLocation();
		setValid(valid);
		return valid;
	}

	
	private boolean validateLocation() {
		String location = cliLocationEditor.getStringValue();
		if(StringUtils.isBlank(location)) {
			return true;
		}
		File file = new File(location);
		if(!OC_BINARY_NAME.equals(file.getName())) {
			setErrorMessage(NLS.bind("{0} is not the openshift 'oc' executable.", file));
			return false;
		}
		if(!file.exists()) {
			setErrorMessage(NLS.bind("{0} was not found.", file));
			return false;
		}
		if(!file.canExecute()) {
			setErrorMessage(NLS.bind("{0} does not have execute permissions.", file));
			return false;
		}
		return true;
	}
	
	private String findOCLocation() {
		String location = "";
		if(SystemUtils.IS_OS_WINDOWS) {
			String[] paths = StringUtils.split(System.getenv("PATH"), ";");
			for (String path : paths) {
				Collection<File> files = FileUtils.listFiles(new File(path), new IOFileFilter() {

					@Override
					public boolean accept(File file) {
						return OC_BINARY_NAME.equals(file.getName());
					}

					@Override
					public boolean accept(File dir, String name) {
						return OC_BINARY_NAME.equals(name);
					}
					
				}, null);
				if(files.size() > 0) {
					location = files.iterator().next().toString();
					break;
				}
			}
		}else {
			ProcessBuilder builder = new ProcessBuilder("which",OC_BINARY_NAME);
			try {
				Process process = builder.start();
				if(process.waitFor(500, TimeUnit.MILLISECONDS) && process.exitValue() == SUCCESS) {
					location = IOUtils.toString(process.getInputStream());
				}
			} catch (IOException e) {
				OpenShiftUIActivator.getDefault().getLogger().logWarning("Unable to find the openshift binary.");
			} catch (InterruptedException e) {
				OpenShiftUIActivator.getDefault().getLogger().logWarning("Unable to find the openshift binary.");
			}
		}
		return StringUtils.trim(location);
	}
	
}