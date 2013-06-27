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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;

/**
 * @author André Dietisheim
 */
public class NoSSHKeysWizard extends Wizard {

	private Connection user;

	public NoSSHKeysWizard(Connection user) {
		this.user = user;
		setWindowTitle("No SSH Keys");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		addPage(new NoSSHKeysWizardPage(user, this));
	}

	private class NoSSHKeysWizardPage extends ManageSSHKeysWizardPage {

		NoSSHKeysWizardPage(Connection connection, IWizard wizard) {
			super("Add SSH Keys",
					"Please make sure you have an SSH public key uploaded to your OpenShift account " + connection.getUsername(),
					"NoSSHKeysPage", connection, wizard);
		}

		@Override
		protected void doCreateControls(Composite parent, DataBindingContext dbc) {
			Label dummyLabel = new Label(parent, SWT.None);
			ValueBindingBuilder
					.bind(WidgetProperties.enabled().observe(dummyLabel))
					.notUpdatingParticipant()
					.to(BeanProperties.value(SSHKeysWizardPageModel.PROPERTY_HAS_KEY).observe(getPageModel()))
					.validatingAfterGet(new IValidator() {

						@Override
						public IStatus validate(Object value) {
							if (Boolean.TRUE.equals(value)) {
								return ValidationStatus.ok();
							} else {
								return ValidationStatus.cancel(
										NLS.bind("You have no SSH public keys in your OpenShift account\n"
												+ "{0} yet, please add your key(s) or\n"
												+ "create new one(s)", getPageModel().getConnection().getUsername()));
							}
						}
					})
					.in(dbc);
			super.doCreateControls(parent, dbc);
		}
	}
}
