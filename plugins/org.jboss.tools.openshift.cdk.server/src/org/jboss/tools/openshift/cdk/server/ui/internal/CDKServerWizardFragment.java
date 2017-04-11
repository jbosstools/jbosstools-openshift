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
import org.eclipse.swt.widgets.FileDialog;
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
	protected IWizardHandle handle;
	protected String homeDir;
	protected Text homeText;
	protected Button browseButton;
	protected String selectedUser = null;
	protected ChooseCredentialComponent credentials;
	
	
	@Override
	public boolean hasComposite() {
		return true;
	}
	

	@Override
	public boolean isComplete() {
		// Only one instance created per workspace, so we need to workaround this
		boolean b = browseButton != null && !browseButton.isDisposed() && findError() == null && super.isComplete();
		return b;
	}
	
	public ImageDescriptor getImageDescriptor() {
		return CDKCoreActivator.getDefault().getSharedImages().descriptor(CDKCoreActivator.CDK_WIZBAN);
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing a Red Hat Container Development Kit installation folder containing a Vagrantfile.";
		String label = "Folder: ";
		return createComposite(parent, handle, title, desc, label);
	}
	
	
	protected Composite setupComposite(Composite parent,  IWizardHandle handle, 
										String title, String desc) {
		// boilerplate
		this.handle = handle;
		Composite main = new Composite(parent, SWT.NONE);
		handle.setTitle(title);
		handle.setDescription(desc);
		handle.setImageDescriptor(getImageDescriptor());
		main.setLayout(new GridLayout(3, false));
		return main;
	}
	
	protected void createCredentialWidgets(Composite main) {
		// create credentials row
		selectedUser = null;
		credentials = new ChooseCredentialComponent(new String[]{CredentialService.REDHAT_ACCESS});
		credentials.addCredentialListener(new ICredentialCompositeListener() {
			@Override
			public void credentialsChanged() {
				selectedUser = credentials.getUser();
				validate();
			}
		});
		credentials.create(main);
		credentials.gridLayout(3);
		selectedUser = credentials.getUser();
	}
	
	protected void createLocationWidgets(Composite main, String homeLabel) {

		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText(homeLabel);
		GridData homeData = new GridData();
		homeData.grabExcessHorizontalSpace = true;
		homeData.horizontalAlignment = SWT.FILL;
		homeText = new Text(main, SWT.BORDER);
		homeText.setLayoutData(homeData);
		browseButton = new Button(main, SWT.PUSH);
		browseButton.setText("Browse...");
		
		homeText.addModifyListener(createHomeModifyListener());
		browseButton.addSelectionListener(createBrowseListener());
		
		fillTextField();
	}

	protected SelectionListener createBrowseListener() {
		return new BrowseListener();
	}
	
	protected ModifyListener createHomeModifyListener() {
		return new HomeModifyListener();
	}
	
	protected class BrowseListener implements SelectionListener {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			browseHomeDirClicked();
			validate();
		}
	}

	protected class HomeModifyListener implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			homeDir = homeText.getText();
			validate();
		}
	}
	
	protected void validateAndPack(Composite main) {
		String err = findError();
		setComplete(err == null);
		handle.update();
		main.pack(true);
	}
	protected Composite createComposite(Composite parent, IWizardHandle handle,
			String title, String desc, String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createCredentialWidgets(main);
		createLocationWidgets(main, homeLabel);
		validateAndPack(main);
		return main;
	}
	
	protected void fillTextField() {
		if( homeDir != null ) {
			homeText.setText(homeDir);
		}
	}
	protected void validate() {
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

	protected String findWarning() {
		return null;
	}
	
	protected String findError() {
		if( homeDir == null || !(new File(homeDir)).exists()) {
			return "The selected folder does not exist.";
		}
		if( !(new File(homeDir, "Vagrantfile").exists())) {
			return "The selected folder does not have a Vagrantfile";
		}
		if( credentials.getDomain() == null || credentials.getUser() == null) {
			return "The Container Development Environment Server Adapter requries Red Hat Access credentials.";
		}
		return null;
	}
	
	protected void browseHomeDirClicked() {
		browseHomeDirClicked(true);
	}
	protected void browseHomeDirClicked(boolean folder) {
		File file = homeDir == null ? null : new File(homeDir);
		if (file != null && !file.exists()) {
			file = null;
		}

		File f = null;
		if( folder ) 
			f = getDirectory(file, homeText.getShell());
		else
			f = getFile(file, homeText.getShell());
		
		if (f != null) {
			homeDir = f.getAbsolutePath();
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

	protected static File getFile(File startingDirectory, Shell shell) {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
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
	

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		exit();
		IServer s = getServerFromTaskModel();
		if( s instanceof IServerWorkingCopy ) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CDKServer.PROP_FOLDER, homeDir);
			swc.setAttribute(CDKServer.PROP_USERNAME, selectedUser);
		}
	}
	
	
}
