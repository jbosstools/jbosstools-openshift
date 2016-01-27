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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DirectoryValidator;
import org.jboss.tools.common.ui.databinding.FileNameValidator;
import org.jboss.tools.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.databinding.SSHPublicKeyNameValidator;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

import com.openshift.client.IOpenShiftSSHKey;
import com.openshift.client.SSHKeyType;

/**
 * @author André Dietisheim
 */
public class NewSSHKeyWizardPage extends AbstractOpenShiftWizardPage {

	private NewSSHKeyWizardPageModel pageModel;

	public NewSSHKeyWizardPage(ExpressConnection user, IWizard wizard) {
		super("Add new SSH key", "Add a new SSH key to your OpenShift user " + user.getUsername(),
				"NewSSHKeysPage", wizard);
		this.pageModel = new NewSSHKeyWizardPageModel(user);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group newSSHKeyGroup = new Group(parent, SWT.NONE);
		newSSHKeyGroup.setText("New SSH Key");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(newSSHKeyGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(4).margins(6, 6).applyTo(newSSHKeyGroup);

		// name
		Label nameLabel = new Label(newSSHKeyGroup, SWT.NONE);
		nameLabel.setText("Name:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
				.validatingAfterConvert(new SSHPublicKeyNameValidator(pageModel))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_NAME).observe(pageModel))
				.notUpdatingParticipant()
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// type
		Label typeLabel = new Label(newSSHKeyGroup, SWT.NONE);
		typeLabel.setText("Key Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(typeLabel);

		ComboViewer typeCombo = new ComboViewer(newSSHKeyGroup, SWT.READ_ONLY);
		typeCombo.setContentProvider(ArrayContentProvider.getInstance());
		typeCombo.setInput(SSHKeyType.values());
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(typeCombo.getControl());
		ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(typeCombo))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_TYPE).observe(pageModel))
				.in(dbc);

		Label fillerLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(fillerLabel);

		// ssh2 home
		Label ssh2HomeLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(ssh2HomeLabel);
		ssh2HomeLabel.setText("SSH2 Home:");

		Text ssh2HomeText = new Text(newSSHKeyGroup, SWT.BORDER);
		ssh2HomeText.setEditable(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(ssh2HomeText);
		Binding ssh2HomeBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(ssh2HomeText))
				.validatingAfterConvert(new DirectoryValidator("ssh2 home directory"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_SSH2_HOME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				ssh2HomeBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Button ssh2HomeBrowseButton = new Button(newSSHKeyGroup, SWT.PUSH);
		ssh2HomeBrowseButton.setText("Browse...");
		ssh2HomeBrowseButton.addSelectionListener(onBrowse(ssh2HomeText));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(ssh2HomeBrowseButton);

		Button defaultSSH2HomeHomeButton = new Button(newSSHKeyGroup, SWT.CHECK);
		defaultSSH2HomeHomeButton.setText("Default");
		defaultSSH2HomeHomeButton.addSelectionListener(onDefault(ssh2HomeText, ssh2HomeBrowseButton));
		defaultSSH2HomeHomeButton.setSelection(true);
		updateSSH2HomeWidgets(true, ssh2HomeText, ssh2HomeBrowseButton);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(ssh2HomeBrowseButton);

		// private key
		Label privateKeyLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(privateKeyLabel);
		privateKeyLabel.setText("Private Key File Name:");

		Text privateKeyText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(privateKeyText);
		Binding privateKeyBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(privateKeyText))
				.validatingAfterConvert(new FileNameValidator("Private Key File Name"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PRIVATEKEY_FILENAME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				privateKeyBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		
		// Passphrase
		Label passphraseLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passphraseLabel);
		passphraseLabel.setText("Private Key Passphrase:");

		Text passphraseText = new Text(newSSHKeyGroup, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(passphraseText);
		ISWTObservableValue passphraseObservable = WidgetProperties.text(SWT.Modify).observe(passphraseText);
		ValueBindingBuilder
				.bind(passphraseObservable)
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PRIVATEKEY_PASSPHRASE).observe(pageModel))
				.in(dbc);
		
		// Passphrase Confirmation
		Label passphraseConfirmLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passphraseLabel);
		passphraseConfirmLabel.setText("Confirm Private Key Passphrase:");

		Text passphraseConfirmText = new Text(newSSHKeyGroup, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(passphraseConfirmText);
		ISWTObservableValue passphraseConfirmObservable = WidgetProperties.text(SWT.Modify).observe(passphraseConfirmText);
		ValueBindingBuilder
				.bind(passphraseConfirmObservable)
				.to(BeanProperties.value(
						NewSSHKeyWizardPageModel.PROPERTY_PRIVATEKEY_CONFIRM_PASSPHRASE).observe(pageModel))
				.in(dbc);
		
		PassPhraseConfirmValidator passPhraseConfrimValidator = 
				new PassPhraseConfirmValidator(passphraseObservable, passphraseConfirmObservable);
		dbc.addValidationStatusProvider(passPhraseConfrimValidator);
		ControlDecorationSupport.create(passPhraseConfrimValidator, SWT.LEFT | SWT.TOP);
		
		// public key
		Label publicKeyLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(publicKeyLabel);
		publicKeyLabel.setText("Public Key File Name:");

		Text publicKeyText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(publicKeyText);
		Binding publicKeyBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(publicKeyText))
				.validatingAfterConvert(new FileNameValidator("Public Key File Name"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PUBLICKEY_FILENAME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				publicKeyBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Link sshPrefsLink = new Link(parent, SWT.NONE);
		sshPrefsLink
				.setText("The private key of your new SSH key pair will get added to the \n<a>SSH2 Preferences</a>");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());

	}	

	private SelectionListener onBrowse(final Text ssh2HomeText) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
				dialog.setFilterPath(getFilterPath(ssh2HomeText.getText()));
				String ssh2HomePath = null;
				if ((ssh2HomePath = dialog.open()) != null) {
					pageModel.setSSH2Home(ssh2HomePath);
				}
			}

			private String getFilterPath(String currentFilterPath) {
				if (StringUtils.isEmpty(currentFilterPath)) {
					return currentFilterPath;
				} else {
					return SSHUtils.getSSH2Home();
				}
			}
		};
	}

	private SelectionListener onDefault(final Text ssh2HomeText, final Button ssh2HomeBrowseButton) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSSH2HomeWidgets(((Button) e.widget).getSelection(), ssh2HomeText, ssh2HomeBrowseButton);
			}
		};
	}

	private void updateSSH2HomeWidgets(boolean isDefault, final Text ssh2HomeText, final Button ssh2HomeBrowseButton) {
		ssh2HomeText.setEnabled(!isDefault);
		ssh2HomeBrowseButton.setEnabled(!isDefault);
		if (isDefault) {
			ssh2HomeText.setText(SSHUtils.getSSH2Home());
		}
	}

	public IStatus addConfiguredSSHKey() {
		try {
			return WizardUtils.runInWizard(new AddSSHKeyJob(pageModel), getContainer());
		} catch (Exception e) {
			return ExpressUIActivator.createErrorStatus("Could not add ssh key " + pageModel.getName() + ".");
		}
	}

	private SelectionAdapter onSshPrefs() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SSHUtils.openPreferencesPage(getShell());
			}
		};
	}

	public IOpenShiftSSHKey getSSHKey() {
		return pageModel.getSSHKey();
	}
	
	/**
	 * Validates a given passphrase confirmation value such that it matches the 
	 * model's passphrase value
	 */
	public class PassPhraseConfirmValidator extends MultiValidator {

		private IObservableValue passphrase;
		private IObservableValue passphraseConfirm;

		public PassPhraseConfirmValidator(IObservableValue passphrase,
				IObservableValue passphraseConfirm) {
			this.passphrase = passphrase;
			this.passphraseConfirm = passphraseConfirm;
		}

		@Override
		protected IStatus validate() {
			Object o1 = passphrase.getValue();
			Object o2 = passphraseConfirm.getValue();
			boolean bothEmpty = (o1 == null || "".equals(o1)) && (o2 == null || "".equals(o2));
			if( !bothEmpty ) {
				// At least one is not null.  if non-null object NOT EQUAL possibly-null object, show error
				if( !(o1 == null ? o2 : o1).equals( o1 == null ? o1 :o2)) 
					return ValidationStatus.error("Please ensure the two passphrases match.");
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(passphraseConfirm);
			return targets;
		}
	}

}
