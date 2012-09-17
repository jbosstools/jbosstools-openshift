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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class AddSSHKeyWizardPage extends AbstractOpenShiftWizardPage {

	private UserDelegate user;

	public AddSSHKeyWizardPage(UserDelegate user, IWizard wizard) {
		super("Add existing SSH Key", "Add an exiting SSH key to your OpenShift account",
				"AddSSHKeysPage", wizard);
		this.user = user;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group addSSHKeyGroup = new Group(parent, SWT.NONE);
		addSSHKeyGroup.setText("Add existing SSH Key");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(addSSHKeyGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(3).margins(6, 6).applyTo(addSSHKeyGroup);

		Label nameLabel = new Label(addSSHKeyGroup, SWT.NONE);
		nameLabel.setText("Name:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(addSSHKeyGroup, SWT.BORDER);
		nameText.setEditable(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1).applyTo(nameText);

		Label fileLabel = new Label(addSSHKeyGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(fileLabel);
		fileLabel.setText("SSH Key:");

		Text fileText = new Text(addSSHKeyGroup, SWT.BORDER);
		fileText.setEditable(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(fileText);

		Button browseButton = new Button(addSSHKeyGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(browseButton);
		browseButton.setText("Browse...");
	}
}