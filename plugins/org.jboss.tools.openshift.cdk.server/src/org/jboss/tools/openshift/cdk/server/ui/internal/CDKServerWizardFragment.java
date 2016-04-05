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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.jboss.tools.foundation.ui.credentials.ChooseCredentialComponent;
import org.jboss.tools.foundation.ui.credentials.ICredentialCompositeListener;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;

public class CDKServerWizardFragment extends WizardFragment {
	private IWizardHandle handle;
	private String homeDir;
	private Text homeText;
	private Button browseButton;
	private ChooseCredentialComponent credentials;
	
	
	public boolean hasComposite() {
		return true;
	}
	

	public boolean isComplete() {
		// Only one instance created per workspace, so we need to workaround this
		return browseButton != null && !browseButton.isDisposed() && super.isComplete();
	}
	
	public ImageDescriptor getImageDescriptor() {
		return CDKCoreActivator.getDefault().getSharedImages().descriptor(CDKCoreActivator.CDK_WIZBAN);
	}

	public Composite createComposite(Composite parent, IWizardHandle handle) {
		this.handle = handle;
		Composite main = new Composite(parent, SWT.NONE);
		handle.setTitle("CDK Server Adapter");
		handle.setDescription("A server adapter representing a CDK installation folder containing a Vagrantfile.");
		handle.setImageDescriptor(getImageDescriptor());
		main.setLayout(new GridLayout(3, false));
		
		
		credentials = new ChooseCredentialComponent(new String[]{CredentialService.REDHAT_ACCESS});
		credentials.addCredentialListener(new ICredentialCompositeListener() {
			public void credentialsChanged() {
				validate();
			}
		});
		credentials.create(main);
		credentials.gridLayout(3);
		
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Folder: ");
		GridData homeData = new GridData();
		homeData.grabExcessHorizontalSpace = true;
		homeData.horizontalAlignment = SWT.FILL;
		homeText = new Text(main, SWT.BORDER);
		homeText.setLayoutData(homeData);
		browseButton = new Button(main, SWT.PUSH);
		browseButton.setText("Browse...");
		

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
		

		
		
		String err = findError();
		setComplete(err == null);
		handle.update();
		main.pack(true);
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
