/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.common.FileUtils;
import org.jboss.tools.openshift.express.internal.ui.common.StringUtils;

/**
 * @author André Dietisheim
 */
public class NewDomainWizardPage extends AbstractOpenShiftWizardPage {

	private static final String FILTEREXPRESSION_PUBLIC_SSH_KEY = "*.pub";
	private static final String FILTERNAME_PUBLIC_SSH_KEY = "Public ssh key file (*.pub)";

	private NewDomainWizardPageModel model;

	public NewDomainWizardPage(String namespace, NewDomainWizardPageModel model, IWizard wizard) {
		super("Domain", "Create a new domain", "New Domain", wizard);
		this.model = model;
	}

	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);

		Label namespaceLabel = new Label(container, SWT.NONE);
		namespaceLabel.setText("&Domain name");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(namespaceLabel);
		Text namespaceText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(namespaceText);
		DataBindingUtils.bindMandatoryTextField(
				namespaceText, "Domain name", NewDomainWizardPageModel.PROPERTY_NAMESPACE, model, dbc);

		Label sshKeyLabel = new Label(container, SWT.NONE);
		sshKeyLabel.setText("SSH Public Key");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(sshKeyLabel);
		Text sshKeyText = new Text(container, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(sshKeyText);
		Binding sshKeyTextBinding = dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(sshKeyText),
				BeanProperties.value(NewDomainWizardPageModel.PROPERTY_SSHKEY).observe(model),
				new UpdateValueStrategy().setAfterGetValidator(new MandatoryStringValidator(
						"You have to select a ssh public key")),
				new UpdateValueStrategy().setAfterGetValidator(new SSHKeyValidator()));
		ControlDecorationSupport.create(sshKeyTextBinding, SWT.TOP | SWT.LEFT);

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseButton);
		browseButton.addSelectionListener(onBrowse());

		Button createButton = new Button(container, SWT.PUSH);
		createButton.setText("New");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(createButton);
		createButton.addSelectionListener(onNew());

		new Label(container, SWT.NONE); // spacer
		Link sshPrefsLink = new Link(container, SWT.NONE);
		sshPrefsLink
				.setText("Please make sure that your private key for the public key is listed in the <a>SSH2 Preferences</a>");
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());

	}

	private SelectionListener onNew() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				InputDialog dialog = new PassphraseDialog(getShell());
				if (Dialog.OK == dialog.open()) {
					try {
						String passPhrase = dialog.getValue();
						model.createSShKeyPair(passPhrase);
					} catch (FileNotFoundException ex) {
						IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Could not read the ssh key folder", ex);
						OpenShiftUIActivator.log(status);
						ErrorDialog.openError(getShell(),
								"Error creating a new ssh key pair",
								"Could not create a new ssh key pair", status);
					} catch (OpenShiftException ex) {
						IStatus status = new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Could not create an ssh key pair", ex);
						OpenShiftUIActivator.log(status);
						ErrorDialog.openError(getShell(),
								"Error creating a new ssh key pair",
								"Could not create a new ssh key pair", status);
					}
				}
			}
		};
	}

	private SelectionListener onBrowse() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				FileDialog dialog = new FileDialog(shell);
				try {
					dialog.setFilterPath(SshPrivateKeysPreferences.getSshKeyDirectory());
				} catch (FileNotFoundException ex) {
					// ignore
				}
				dialog.setFilterNames(new String[] { FILTERNAME_PUBLIC_SSH_KEY });
				dialog.setFilterExtensions(new String[] { FILTEREXPRESSION_PUBLIC_SSH_KEY });
				String sshKeyPath = dialog.open();
				if (sshKeyPath != null) {
					model.setSshKey(sshKeyPath);
				}
			}
		};
	}

	private SelectionAdapter onSshPrefs() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SshPrivateKeysPreferences.openPreferencesPage(getShell());
				// refresh warning about key 
				// (since user may have changed SSH2 prefs)
				getDataBindingContext().updateTargets();
			}
		};
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR, this, dbc);
	}

	private class SSHKeyValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			if (!(value instanceof String)
					|| StringUtils.isEmpty((String) value)
					|| !FileUtils.canRead((String) value)) {
				return ValidationStatus.error("You have to provide a valid ssh public key");
			}
			if (!isKeyKnownToSsh((String) value)) {
				return ValidationStatus.warning(
						NLS.bind("Could not find the private portion for your public key. "
								+ "Make sure it is listed in the ssh2 preferences.", value));
			}
			return ValidationStatus.ok();
		}

		private boolean isKeyKnownToSsh(String publicKeyPath) {
			if (StringUtils.isEmpty(publicKeyPath)) {
				return false;
			}
			for (String preferencesKey : SshPrivateKeysPreferences.getKeys()) {
				try {
					File privateKey = SshPrivateKeysPreferences.getKeyFile(preferencesKey);
					if (privateKey == null
							|| !FileUtils.canRead(privateKey)) {
						continue;
					}
					if (publicKeyPath.startsWith(privateKey.getAbsolutePath() + ".")) {
						return true;
					}
				} catch (FileNotFoundException e) {
					continue;
				}
			}
			return false;
		}
	}

}
