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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import org.jboss.tools.foundation.ui.credentials.ChooseCredentialComposite;
import org.jboss.tools.foundation.ui.credentials.ICredentialCompositeListener;
import org.jboss.tools.foundation.ui.util.FormDataUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

/*
 * I would love to remake this class to use a credentialing framework
 * So users can select the username and password from a central location
 */
public class CDKCredentialSection extends ServerEditorSection {
	private Button passCredentialsButton;
	private SelectionListener passCredentialsListener;
	private ChooseCredentialComposite2 credentialComposite;
	private Text envUserText, envPassText;
	private ModifyListener envUserListener, envPassListener;
	
	public CDKCredentialSection() {
		// TODO Auto-generated constructor stub
	}
	
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
	}
	
	public void createSection(Composite parent) {
		super.createSection(parent);
		CDKServer cdkServer = (CDKServer)server.getOriginal().loadAdapter(CDKServer.class, new NullProgressMonitor());
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE|ExpandableComposite.EXPANDED|ExpandableComposite.TITLE_BAR);
		section.setText("Credentials");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		Composite composite = toolkit.createComposite(section);
		composite.setLayout(new FormLayout());

		passCredentialsButton = toolkit.createButton(composite, "Pass credentials to environment", SWT.CHECK);
		passCredentialsButton.setSelection(cdkServer.getServer().getAttribute(CDKServer.PROP_PASS_CREDENTIALS, false));
		passCredentialsListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				execute(new SetPassCredentialsCommand(server));
			}
		};
		passCredentialsButton.addSelectionListener(passCredentialsListener);
		
		
		credentialComposite = createChooseCredentialComposite(composite);
		credentialComposite.addCredentialListener(new ICredentialCompositeListener() {
			public void credentialsChanged() {
				execute(new SetUsernameCommand(server));
			}
		});

		Label userEnvLabel = toolkit.createLabel(composite, "Username Environment Variable: ");
		envUserText = toolkit.createText(composite, server.getAttribute(CDKServer.PROP_USER_ENV_VAR, CDKServer.SUB_USERNAME));
		
		Label passEnvLabel = toolkit.createLabel(composite, "Password Environment Variable: ");
		envPassText = toolkit.createText(composite, server.getAttribute(CDKServer.PROP_PASS_ENV_VAR, CDKServer.SUB_PASSWORD));
		
		envUserListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new SetUsernameVariableCommand(server));
			}
		};
		envUserText.addModifyListener(envUserListener);
		
		envPassListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new SetPasswordVariableCommand(server));
			}
		};
		envPassText.addModifyListener(envPassListener);
		
		
		// Layout the widgets
		passCredentialsButton.setLayoutData(FormDataUtility.createFormData2(0,5,null,0,0,5,100,-5));
		credentialComposite.setLayoutData(FormDataUtility.createFormData2(passCredentialsButton,5,null,0,0,5,100,-5));
		userEnvLabel.setLayoutData(FormDataUtility.createFormData2(credentialComposite,8,null,0,0,5,null,0));
		envUserText.setLayoutData(FormDataUtility.createFormData2(credentialComposite,5,null,0,userEnvLabel,5,100,-5));
		passEnvLabel.setLayoutData(FormDataUtility.createFormData2(envUserText,8,null,0,0,5,null,0));
		envPassText.setLayoutData(FormDataUtility.createFormData2(envUserText,5,null,0,passEnvLabel,5,100,-5));
		
		
		toolkit.paintBordersFor(composite);
		section.setClient(composite);
	}
	
	private class ChooseCredentialComposite2 extends ChooseCredentialComposite {
		public ChooseCredentialComposite2(Composite parent, String[] domains, String selectedUsername) {
			super(parent, domains, selectedUsername);
		}
		public Combo getUserCombo() {
			return userCombo;
		}
		public SelectionListener getUserListener() {
			return userComboListener;
		}
	}
	private ChooseCredentialComposite2 createChooseCredentialComposite(Composite parent) {
		String initialUsername = server.getAttribute(CDKServer.PROP_USERNAME, (String)null);
		final ChooseCredentialComposite2 comp = new ChooseCredentialComposite2(
				parent, 
				new String[]{CredentialService.REDHAT_ACCESS},
				initialUsername) {
			
		};
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
		protected void postOp(int type) {
			boolean pass = wc.getAttribute(CDKServer.PROP_PASS_CREDENTIALS, true);
			envUserText.setEnabled(pass);
			envPassText.setEnabled(pass);
			credentialComposite.setEnabled(pass);
		}

	}
}
