/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyButtonCommand;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDK32CredentialSection extends CDKCredentialSection {

	public CDK32CredentialSection() {
		// TODO Auto-generated constructor stub
	}

	protected Composite createWidgets(Composite parent, CDKServer cdkServer, FormToolkit toolkit) {
		Composite composite = initializeSectionWidget(parent, toolkit);
		createChooseCredentialsWidgets(composite, toolkit);
		createEnvironmentWidgets(composite, toolkit, cdkServer);
		addRegistrationWidgets(composite, cdkServer, toolkit);
		return composite;
	}

	private SelectionAdapter registerListener, unregisterListener;
	private Button regBtn, unregBtn;

	protected void addRegistrationWidgets(Composite parent, CDKServer cdkServer, FormToolkit toolkit) {
		Label registrationLbl = toolkit.createLabel(parent, "Registration: ");
		toolkit.createLabel(parent, "");
		regBtn = toolkit.createButton(parent, "Add --skip-registration flag when starting", SWT.CHECK);
		toolkit.createLabel(parent, "");
		unregBtn = toolkit.createButton(parent, "Add --skip-unregistration flag when stopping", SWT.CHECK);
		GridDataFactory.generate(registrationLbl, new Point(3, 1));
		GridDataFactory.defaultsFor(regBtn).span(new Point(2, 1)).applyTo(regBtn);
		GridDataFactory.defaultsFor(unregBtn).span(new Point(2, 1)).applyTo(regBtn);

		boolean skipRegInitial = cdkServer.skipRegistration();
		boolean skipUnregInitial = cdkServer.skipUnregistration();
		regBtn.setSelection(skipRegInitial);
		unregBtn.setSelection(skipUnregInitial);

		registerListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new SetSkipRegistrationCommand(server));
			}
		};
		regBtn.addSelectionListener(registerListener);

		unregisterListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new SetSkipUnregistrationCommand(server));
			}
		};
		unregBtn.addSelectionListener(unregisterListener);
	}

	public class SetSkipRegistrationCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetSkipRegistrationCommand(IServerWorkingCopy server) {
			super(server, "Toggle --skip-registration flag", regBtn, regBtn.getSelection(), CDKServer.PROP_SKIP_REG,
					registerListener);
		}
	}

	public class SetSkipUnregistrationCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetSkipUnregistrationCommand(IServerWorkingCopy server) {
			super(server, "Toggle --skip-unregistration flag", unregBtn, unregBtn.getSelection(),
					CDKServer.PROP_SKIP_UNREG, unregisterListener);
		}
	}

}
