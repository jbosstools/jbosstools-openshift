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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;
import org.jboss.tools.openshift.internal.core.preferences.OCBinaryValidator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.DOWNLOAD_INSTRUCTIONS_URL;

/**
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private CliFileEditor cliLocationEditor;
	private OCBinary ocBinary;
	private Label ocVersionLabel;
	private Composite ocMessageComposite;
	private Label ocMessageLabel;
	
	public OpenShiftPreferencePage() {
		super(GRID);
		this.ocBinary = OCBinary.getInstance();
	}
	
	@Override
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
		
		ocVersionLabel = new Label(getFieldEditorParent(), SWT.WRAP);
		GridDataFactory.fillDefaults().span(3, 1).hint(300, SWT.DEFAULT).applyTo(ocVersionLabel);
		ocMessageComposite = new Composite(getFieldEditorParent(), SWT.NONE);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(ocMessageComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ocMessageComposite);
        Label label = new Label(ocMessageComposite, SWT.NONE);
        label.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(label);
        ocMessageLabel = new Label(ocMessageComposite, SWT.WRAP);
        GridDataFactory.fillDefaults().hint(300, 60).grab(true, true).applyTo(ocMessageLabel);
        ocMessageComposite.setVisible(false);
    }
	
	@Override
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
		Job job = new UIUpdatingJob("Checking oc binary") {
 
		    private String version;
		    
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                version = new OCBinaryValidator(location).getVersion(monitor);
                return Status.OK_STATUS;
            }

            @Override
            protected IStatus updateUI(IProgressMonitor monitor) {
                if (!ocMessageComposite.isDisposed() && !monitor.isCanceled()) {
                    ocMessageLabel.setText(NLS.bind("Your client version is {0}. OpenShift client version 1.1.1 or higher is required to avoid rsync issues.", version));
                    ocMessageComposite.setVisible(!OCBinaryValidator.isCompatibleForPublishing(version));
                }
                return super.updateUI(monitor);
            }
        };
        job.schedule();
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
	}
}