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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.NonEmptyStringValidator;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.SSHUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class NewSSHKeyWizardPage extends AbstractOpenShiftWizardPage {

	private NewSSHKeyWizardPageModel pageModel;

	public NewSSHKeyWizardPage(UserDelegate user, IWizard wizard) {
		super("Add new SSH key", "Add a new SSH key to your OpenShift account",
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
				.numColumns(3).margins(6, 6).applyTo(newSSHKeyGroup);

		Label nameLabel = new Label(newSSHKeyGroup, SWT.NONE);
		nameLabel.setText("Name:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
				.validatingAfterConvert(new SSHPublicKeyNameValidator(pageModel))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_NAME).observe(pageModel))
				.notUpdatingParticipant()
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

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
				.validatingAfterConvert(new NonEmptyStringValidator("ssh2 home directory"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_SSH2_HOME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				ssh2HomeBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Button ss2HomeBrowseButton = new Button(newSSHKeyGroup, SWT.PUSH);
		ss2HomeBrowseButton.setText("Browse...");
		ss2HomeBrowseButton.addSelectionListener(onBrowse());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(ss2HomeBrowseButton);

		Label privateKeyLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(privateKeyLabel);
		privateKeyLabel.setText("Private Key:");

		Text privateKeyText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(privateKeyText);
		Binding privateKeyBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(privateKeyText))
				.validatingAfterConvert(new NonEmptyStringValidator("private key file name"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PRIVATEKEY_PATH).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				privateKeyBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Label passphraseLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(passphraseLabel);
		passphraseLabel.setText("Private Key Passphrase:");

		Text passphraseText = new Text(newSSHKeyGroup, SWT.BORDER | SWT.PASSWORD);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(passphraseText);
		ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(passphraseText))
				.validatingAfterConvert(new NonEmptyStringValidator("pass phrase"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PRIVATEKEY_PASSPHRASE).observe(pageModel))
				.in(dbc);

		Label publicKeyLabel = new Label(newSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(publicKeyLabel);
		publicKeyLabel.setText("Public Key:");

		Text publicKeyText = new Text(newSSHKeyGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(publicKeyText);
		Binding publicKeyBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(publicKeyText))
				.validatingAfterConvert(new NonEmptyStringValidator("public key file name"))
				.to(BeanProperties.value(NewSSHKeyWizardPageModel.PROPERTY_PUBLICKEY_PATH).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				publicKeyBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}

	private SelectionListener onBrowse() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
				dialog.setFilterPath(SSHUtils.getSSH2Home());
				String filePath = null;
				if ((filePath = dialog.open()) != null) {
					pageModel.setPublicKeyPath(filePath);
				}
			}
		};
	}

	public IStatus addConfiguredSSHKey() {
		try {
			return WizardUtils.runInWizard(new AddSSHKeyJob(pageModel), getContainer());
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus("Could not add ssh key " + pageModel.getName() + ".");
		}
	}
}
