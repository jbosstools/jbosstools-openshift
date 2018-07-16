/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.preferences;

import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.DOWNLOAD_INSTRUCTIONS_URL;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.common.ui.databinding.Status2IconConverter;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.core.OCBinary;
import org.jboss.tools.openshift.internal.core.OCBinaryValidator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.osgi.framework.Version;

/**
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private CliFileEditor cliLocationEditor;
	private OCBinary ocBinary;
	private Label ocVersionLabel;
	private Composite ocMessageComposite;
	private Link ocMessageLabel;
	private Label ocMessageIcon;

	public OpenShiftPreferencePage() {
		super(GRID);
		this.ocBinary = OCBinary.getInstance();
	}

	@Override
	public void createFieldEditors() {
		Link link = new Link(getFieldEditorParent(), SWT.WRAP);
		link.setText(
				"The OpenShift client binary (oc) is required for features such as Port Forwarding or Log Streaming. "
						+ "You can find more information about how to install it from <a>here</a>.");
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.FILL).hint(1, 60).applyTo(link);
		link.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				onDownloadLinkClicked();
			}
		});
		this.cliLocationEditor = new CliFileEditor();
		cliLocationEditor.setFilterPath(SystemUtils.getUserHome());
		cliLocationEditor.setFileExtensions(createFilters(ocBinary.getExtensions()));
		cliLocationEditor.setValidateStrategy(FileFieldEditor.VALIDATE_ON_KEY_STROKE);
		addField(cliLocationEditor);

		ocVersionLabel = new Label(getFieldEditorParent(), SWT.WRAP);
		ocVersionLabel.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().span(3, 1)
			.applyTo(ocVersionLabel);
		createOcMessageLabel(getFieldEditorParent());
	}

	private void createOcMessageLabel(Composite parent) {
		this.ocMessageComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).span(3, 1).grab(true, true)
			.applyTo(ocMessageComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ocMessageComposite);

		this.ocMessageIcon = new Label(ocMessageComposite, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER).applyTo(ocMessageIcon);

		this.ocMessageLabel = new Link(ocMessageComposite, SWT.WRAP);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).indent(0, 10)
			.applyTo(ocMessageLabel);
		ocMessageLabel.addListener(SWT.Selection, event -> {
			if (event.text.startsWith("download")) {
				onDownloadLinkClicked();
			}
		});		
		ocMessageComposite.setVisible(false);
	}

	private void onDownloadLinkClicked() {
		new BrowserUtility().checkedCreateExternalBrowser(DOWNLOAD_INSTRUCTIONS_URL,
				OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
	}

	private String[] createFilters(String[] suffixes) {
		String[] filters = new String[suffixes.length];
		for (int i = 0; i < filters.length; i++) {
			filters[i] = "*" + suffixes[i];
		}
		return filters;
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(OpenShiftUIActivator.getDefault().getCorePreferenceStore());
	}

	@Override
	protected void performDefaults() {
		String location = StringUtils.defaultIfBlank(ocBinary.getSystemPathLocation(), "");
		getPreferenceStore().setDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC, location);

		if (StringUtils.isBlank(location)) {
			String message = NLS.bind("Could not find the OpenShift client executable \"{0}\" on your system path.",
					ocBinary.getName());
			setMessage(message, IMessageProvider.WARNING);
			OpenShiftUIActivator.getDefault().getLogger().logWarning(message);
			return;
		}

		super.performDefaults();

		//Super implementation changes instance value, we need it clean.
		getPreferenceStore().setToDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);
	}

	@Override
	public boolean performOk() {
		boolean valid = false;
		if (cliLocationEditor.getStringValue()
				.equals(getPreferenceStore().getDefaultString(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC))) {
			// super implementation changes instance value, we need it clean.
			getPreferenceStore().setToDefault(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);
		} else {
			valid = super.performOk();
		}
		validateLocation(cliLocationEditor.getStringValue());
		setValid(valid);
		return valid;
	}

	private void validateLocation(final String location) {
		setValid(false);
		ocVersionLabel.setText("Checking OpenShift client version...");
		Job validationJob = new UIUpdatingJob("Checking oc binary...") {

			private OCBinaryValidator validator = new OCBinaryValidator(location);
			private Version version = Version.emptyVersion;
			private IStatus status = Status.OK_STATUS;
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				this.version = validator.getVersion(monitor);
				this.status = validator.getStatus(version);
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				} else {
					return Status.OK_STATUS;
				}
			}

			@Override
			protected IStatus updateUI(IProgressMonitor monitor) {
				if (!ocMessageComposite.isDisposed() 
						&& !monitor.isCanceled()) {
					ocVersionLabel.setText(getOcVersionMessage());
					ocMessageLabel.setText(removePreferencesLink(status.getMessage()));
					Image messageIcon = (Image) new Status2IconConverter().convert(status);
					ocMessageIcon.setImage(messageIcon);
					ocMessageComposite.setVisible(!status.isOK());
					ocMessageComposite.layout(true);
					// always have page valid so that user can always leave the page
					setValid(true);
				}
				return super.updateUI(monitor);
			}

			/**
			 * Removes a link to the preferences that exists in the message. Does nothing
			 * otherwise. We already are in the preferences, so there's no use to have a
			 * link (that opens up the preferences in other places) to configure.
			 * 
			 * @param message
			 * @return the message without the link-markup. Returns the unaltered message
			 *         otherwise.
			 */
			private String removePreferencesLink(String message) {
				if (StringUtils.isEmpty(message)) {
					return message;
				}

				return message.replaceAll(
						OpenShiftCoreMessages.OCBinaryPreferencesLink, 
						OpenShiftCoreMessages.OCBinaryPreferencesDeactivatedLink);
			}

			private String getOcVersionMessage() {
				if (version == null
						|| Version.emptyVersion.equals(version)) {
					return "Could not determine your OpenShift client version.";
				} else {
					return NLS.bind("Your OpenShift client version is {0}.{1}.{2}",
						new Object[] { version.getMajor(), version.getMinor(), version.getMicro() });
				}
			}
		};
		validationJob.schedule();
	}

	class CliFileEditor extends FileFieldEditor {

		private String lastCheckedValue = null;

		public CliFileEditor() {
			//Validation strategy should be set in constructor, later setting it has no effect.
			super(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC,
					NLS.bind("''{0}'' executable location", ocBinary.getName()), false,
					StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		}

		@Override
		protected boolean checkState() {
			String newCheckedValue = getStringValue();
			setMessage("");
			if (!StringUtils.equals(newCheckedValue, lastCheckedValue)) {
				ocVersionLabel.setText("");
				ocMessageComposite.setVisible(false);
				validateLocation(newCheckedValue);
				this.lastCheckedValue = newCheckedValue;
				return true;
			} else {
				return isValid();
			}
		}
	}
}