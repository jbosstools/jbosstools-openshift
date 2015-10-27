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

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.foundation.core.credentials.CredentialService;
import org.jboss.tools.foundation.ui.credentials.ChooseCredentialComposite;
import org.jboss.tools.foundation.ui.credentials.ICredentialCompositeListener;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.ui.internal.util.FormDataUtility;

public class CDKServerWizardFragment extends WizardFragment {
	private IWizardHandle handle;
	private String homeDir;
	private Text homeText;
	private Button browseButton;
	private ChooseCredentialComposite credentials;
	
	
	public boolean hasComposite() {
		return true;
	}
	
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		this.handle = handle;
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new FormLayout());
		handle.setTitle("CDK Server Adapter");
		handle.setDescription("A server adapter representing a CDK installation folder containing a Vagrantfile.");
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Folder: ");
		homeText = new Text(main, SWT.BORDER);
		browseButton = new Button(main, SWT.PUSH);
		browseButton.setText("Browse");
		

		homeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				homeDir = homeText.getText();
				validate();
			}
		});
		browseButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				browseHomeDirClicked();
				validate();
			}

		});
		credentials = new ChooseCredentialComposite(main, new String[]{CredentialService.REDHAT_ACCESS});
		credentials.addCredentialListener(new ICredentialCompositeListener() {
			public void credentialsChanged() {
				validate();
			}
		});
		
		l.setLayoutData(FormDataUtility.createFormData2(0,7,null,0,0,5,null,0));
		homeText.setLayoutData(FormDataUtility.createFormData2(0,5,null,0,l,50,browseButton,-5));
		browseButton.setLayoutData(FormDataUtility.createFormData2(0,5,null,0,null,0,100,-5));
		credentials.setLayoutData(FormDataUtility.createFormData2(browseButton,5,null,0,0,0,100,-5));
		
		String err = findError();
		setComplete(err == null);
		handle.update();
		return main;
	}
	
	private void validate() {
		String err = findError();
		if( err != null ) {
			handle.setMessage(err, IMessageProvider.ERROR);
			setComplete(false);
		} else {
			setComplete(true);
			String warn = findWarning();
			if( warn != null ) {
				handle.setMessage(warn, IMessageProvider.WARNING);
			} else {
				handle.setMessage(null, IMessageProvider.NONE);
			}
		}
		handle.update();
	}

	private String findWarning() {
		return null;
	}
	
	private String findError() {
		if( homeDir == null || !(new File(homeDir)).exists()) {
			return "The selected folder does not exist.";
		}
		if( !(new File(homeDir, "Vagrantfile").exists())) {
			return "The selected folder does not have a Vagrantfile";
		}
		if( credentials.getDomain() == null || credentials.getUser() == null) {
			return "The CDK Server Adapter requries Red Hat Access credentials.";
		}
		return null;
	}
	
	protected void browseHomeDirClicked() {
		File file = homeDir == null ? null : new File(homeDir);
		if (file != null && !file.exists()) {
			file = null;
		}

		File directory = getDirectory(file, homeText.getShell());
		if (directory != null) {
			homeDir = directory.getAbsolutePath();
			homeText.setText(homeDir);
		}
	}
	

	protected static File getDirectory(File startingDirectory, Shell shell) {
		DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
		if (startingDirectory != null) {
			fileDialog.setFilterPath(startingDirectory.getPath());
		}

		String dir = fileDialog.open();
		if (dir != null) {
			dir = dir.trim();
			if (dir.length() > 0) {
				return new File(dir);
			}
		}
		return null;
	}


	protected IServer getServerFromTaskModel() {
		IServer wc = (IServer) getTaskModel().getObject(TaskModel.TASK_SERVER);
		return wc;
	}
	

	public void performFinish(IProgressMonitor monitor) throws CoreException {
		exit();
		IServer s = getServerFromTaskModel();
		if( s instanceof IServerWorkingCopy ) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CDKServer.PROP_FOLDER, homeDir);
		}
	}
	
	
}
