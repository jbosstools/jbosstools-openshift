/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyButtonCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.ui.credentials.ChooseCredentialComponent;
import org.jboss.tools.foundation.ui.credentials.ICredentialCompositeListener;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

/*
 * I would love to remake this class to use a credentialing framework
 * So users can select the username and password from a central location
 */
public class CDKCredentialSection extends ServerEditorSection {
	private Button passCredentialsButton;
	private SelectionListener passCredentialsListener;
	private ChooseCredentialComponent credentialComposite;
	private Text envUserText, envPassText;
	private ModifyListener envUserListener, envPassListener;
	
	public CDKCredentialSection() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
	}
	
	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);
		CDKServer cdkServer = (CDKServer)server.getOriginal().loadAdapter(CDKServer.class, new NullProgressMonitor());
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED|ExpandableComposite.TITLE_BAR);
		section.setText("Credentials");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		Composite composite = toolkit.createComposite(section);
		composite.setLayout(new GridLayout(3, false));

		passCredentialsButton = toolkit.createButton(composite, "Pass credentials to environment", SWT.CHECK);
		passCredentialsButton.setSelection(cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false));
		passCredentialsListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new SetPassCredentialsCommand(server));
			}
		};
		passCredentialsButton.addSelectionListener(passCredentialsListener);
		
		
		credentialComposite = createChooseCredentialComponent(composite);
		credentialComposite.addCredentialListener(new ICredentialCompositeListener() {
			@Override
			public void credentialsChanged() {
				execute(new SetUsernameCommand(server));
			}
		});

		Label environmentVars = toolkit.createLabel(composite, "Environment Variables: ");
		
		Label userEnvLabel = toolkit.createLabel(composite, "Username: ");
		envUserText = toolkit.createText(composite, cdkServer.getUserEnvironmentKey());
		
		Label passEnvLabel = toolkit.createLabel(composite, "Password: ");
		envPassText = toolkit.createText(composite, cdkServer.getPasswordEnvironmentKey());
		
		envUserListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new SetUsernameVariableCommand(server));
			}
		};
		envUserText.addModifyListener(envUserListener);
		
		envPassListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new SetPasswordVariableCommand(server));
			}
		};
		envPassText.addModifyListener(envPassListener);
		
		
		// Layout the widgets
		GridDataFactory.generate(passCredentialsButton, new Point(3,1));
		credentialComposite.gridLayout(3);
		GridDataFactory.generate(environmentVars, new Point(3,1));
		GridDataFactory.generate(envUserText, new Point(2,1));
		GridDataFactory.generate(envPassText, new Point(2,1));
		
		toolkit.paintBordersFor(composite);
		section.setClient(composite);
	}
	
	
	private ChooseCredentialComponent createChooseCredentialComponent(Composite parent) {
		String initialUsername = server.getAttribute(CDKServer.PROP_USERNAME, (String)null);
		final ChooseCredentialComponent comp = new ChooseCredentialComponent(
				new String[]{CredentialService.REDHAT_ACCESS},
				initialUsername);
		comp.create(parent);
		return comp;
	}
	
	public class SetUsernameCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetUsernameCommand(IServerWorkingCopy server) {
			super(server, "Change Username", credentialComposite.getUserCombo(), 
					credentialComposite.getUser(), CDKServer.PROP_USERNAME, credentialComposite.getUserListener());
		}
	}

	public class SetUsernameVariableCommand extends ServerWorkingCopyPropertyCommand {
		public SetUsernameVariableCommand(IServerWorkingCopy server) {
			super(server, "Change Username Variable", envUserText, 
					envUserText.getText(), CDKServer.PROP_USER_ENV_VAR, envUserListener);
		}
	}

	public class SetPasswordVariableCommand extends ServerWorkingCopyPropertyCommand {
		public SetPasswordVariableCommand(IServerWorkingCopy server) {
			super(server, "Change Password Variable", envPassText, 
					envPassText.getText(), CDKServer.PROP_PASS_ENV_VAR, envPassListener);
		}
	}

	
	public class SetPassCredentialsCommand extends ServerWorkingCopyPropertyButtonCommand {
		public SetPassCredentialsCommand(IServerWorkingCopy server) {
			super(server, "Pass credentials to server", passCredentialsButton, passCredentialsButton.getSelection(), 
					CDKServer.PROP_PASS_CREDENTIALS, passCredentialsListener);
		}
		@Override
		protected void postOp(int type) {
			boolean pass = wc.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, true);
			envUserText.setEnabled(pass);
			envPassText.setEnabled(pass);
			credentialComposite.setEnabled(pass);
		}

	}
}
