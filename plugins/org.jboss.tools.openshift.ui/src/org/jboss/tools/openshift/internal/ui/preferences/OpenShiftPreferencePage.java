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
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.common.core.util.ThreadUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author jeff.cantrill
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private static final int WHICH_CMD_TIMEOUT = 10 * 1000;
	private static final int WHICH_CMD_SUCCESS = 0;
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
		Link link = new Link(getFieldEditorParent(), SWT.WRAP );
		link.setText("The OpenShift Client (oc) binary is required for features such as Port Forwarding or Log Streaming. "
				+ "You can find more information about how to install it from <a>here</a>.");
		GridDataFactory.fillDefaults().span(3, 1).hint(300, SWT.DEFAULT).applyTo(link);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new BrowserUtility().checkedCreateExternalBrowser("https://github.com/openshift/origin/blob/master/CONTRIBUTING.adoc#download-from-github", 
																  OpenShiftUIActivator.PLUGIN_ID, 
																  OpenShiftUIActivator.getDefault().getLog());
			}
		});
		this.cliLocationEditor = 
				new FileFieldEditor(
						IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC, 
						NLS.bind("''{0}'' executable location", OC_BINARY_NAME), getFieldEditorParent());
		this.cliLocationEditor.setFilterPath(SystemUtils.getUserHome());
		this.cliLocationEditor.setFileExtensions(EXTENSIONS);
		addField(cliLocationEditor);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        performOk();
    }
	
	public void init(IWorkbench workbench) {
		setPreferenceStore(OpenShiftUIActivator.getDefault().getCorePreferenceStore());
	}
	
	@Override
	protected void performDefaults() {
		String location = findOCLocation();
		if(StringUtils.isBlank(location)) {
			String message = NLS.bind("Could not find the OpenShift Client binary \"{0}\" on your path.", OC_BINARY_NAME);
			OpenShiftUIActivator.getDefault().getLogger().logWarning(message);				
			MessageDialog.openWarning(getShell(), "No OpenShift Client binary", message);
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
			setErrorMessage(NLS.bind("{0} is not the OpenShift Client ''{1}'' executable.", file, OC_BINARY_NAME));
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
		String location = null;
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
			String path = ThreadUtils.runWithTimeout(WHICH_CMD_TIMEOUT, new Callable<String>() {
				@Override
				public String call() throws Exception {
					Process process = null;
					try {
						process = new ProcessBuilder("which", OC_BINARY_NAME).start();
						process.waitFor();
						if(process.exitValue() == WHICH_CMD_SUCCESS) {
							return IOUtils.toString(process.getInputStream());
						};
					} catch (IOException e) {
						OpenShiftUIActivator.getDefault().getLogger().logError("Could not run 'which' command", e);
					} finally {
						if (process != null) {
							process.destroy();
						}
					}
					return null;
				}
			});

			if (!StringUtils.isEmpty(path)) {
				location = path;
			}
		}
		return StringUtils.trim(location);
	}	
}