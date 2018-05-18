/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKConstants;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.VersionUtil;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class CDK32ServerWizardFragment extends CDK3ServerWizardFragment {
	protected String profileName, minishiftHome;
	protected Text profileText;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing Red Hat Container Development Kit Version 3.2+";
		String label = "Minishift Binary: ";
		return createComposite(parent, handle, title, desc, label);
	}

	@Override
	protected Composite createComposite(Composite parent, IWizardHandle handle, String title, String desc,
			String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		if (shouldCreateCredentialWidgets()) {
			addRegistrationLink(main);
			createCredentialWidgets(main);
		}
		createHypervisorWidgets(main);
		createDownloadWidgets(main, handle);
		createLocationWidgets(main, homeLabel);
		createHomeWidgets(main);
		createProfileWidgets(main);
		validateAndPack(main);
		return main;
	}
	protected void createHomeWidgets(Composite main) {
		Label l = new Label(main, SWT.NONE);
		l.setText("Minishift Home:");
		GridData homeData = new GridData();
		homeData.grabExcessHorizontalSpace = true;
		homeData.horizontalAlignment = SWT.FILL;
		homeData.widthHint = 100;
		Text msHomeText = new Text(main, SWT.SINGLE | SWT.BORDER);
		msHomeText.setLayoutData(homeData);
		String defMSHome = getDefaultMinishiftHome();
		msHomeText.setText(defMSHome);
		Button msHomeBrowse = new Button(main, SWT.PUSH);
		msHomeBrowse.setText("Browse...");
		GridData browseData = new GridData();
		browseData.grabExcessHorizontalSpace = true;
		browseData.horizontalAlignment = SWT.FILL;
		msHomeBrowse.setLayoutData(browseData);
		
		SelectionAdapter msHomeSelListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseClicked(msHomeText, FOLDER);
				validate();
			}
		};
		msHomeBrowse.addSelectionListener(msHomeSelListener);
		ModifyListener msHomeModListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				minishiftHome = msHomeText.getText();
			}
		}; 
		msHomeText.addModifyListener(msHomeModListener);
	}	
	
	private String getDefaultMinishiftHome() {
		String msHome = System.getenv(CDK32Server.ENV_MINISHIFT_HOME);
		if( msHome == null || msHome.isEmpty() || !(new File(msHome).exists())) {
			return new Path(System.getProperty("user.home")).append(CDKConstants.CDK_RESOURCE_DOTMINISHIFT).toOSString();
		}
		return msHome;
	}
	protected void createProfileWidgets(Composite main) {
		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText("Minishift Profile:");
		GridData profileData = new GridData();
		profileData.grabExcessHorizontalSpace = true;
		profileData.horizontalAlignment = SWT.FILL;
		profileData.horizontalSpan = 2;
		profileText = new Text(main, SWT.BORDER | SWT.SINGLE);
		profileText.setLayoutData(profileData);
		profileText.setText(CDK32Server.MINISHIFT_DEFAULT_PROFILE);
		profileName = CDK32Server.MINISHIFT_DEFAULT_PROFILE;

		profileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				profileName = profileText.getText();
				validate();
			}
		});
	}

	protected String isVersionCompatible(MinishiftVersions versions) {
		return isCDKVersionCompatible(versions);
	}

	public static String isCDKVersionCompatible(MinishiftVersions versions) {
		return VersionUtil.matchesCDK32(versions);
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		IServer s = getServerFromTaskModel();
		if (s instanceof IServerWorkingCopy) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			if (profileName != null && !profileName.isEmpty()) {
				swc.setAttribute(CDK32Server.PROFILE_ID, profileName);
				swc.setAttribute(CDK3Server.MINISHIFT_HOME, minishiftHome);
			}
		}
	}
	
	
	protected static final boolean FILE = true;
	protected static final boolean FOLDER = false;
	protected void browseClicked(Text text, boolean type) {
		if (text == null)
			return;

		File file = text.getText() == null ? null : new File(text.getText());
		if (file != null && !file.exists()) {
			file = null;
		}

		File f2 = null;
		if (type == FILE)
			f2 = chooseFile(file, text.getShell());
		else if (type == FOLDER)
			f2 = chooseDirectory(file, text.getShell());

		if (f2 != null) {
			String newVal = f2.getAbsolutePath();
			if (newVal != null && !newVal.equals(text.getText())) {
				text.setText(newVal);
			}
		}
	}

	protected static File chooseFile(File startingDirectory, Shell shell) {
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		if (startingDirectory != null) {
			if (startingDirectory.isFile())
				fileDialog.setFilterPath(startingDirectory.getParentFile().getPath());
			else
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

	protected static File chooseDirectory(File startingDirectory, Shell shell) {
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
}
