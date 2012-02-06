/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPageModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.IOpenShiftWizardModel;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class ExpressDetailsComposite {
	public static ExpressDetailsComposite createComposite(Composite parent, 
			IServerModeUICallback callback, String mode, boolean showVerify) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		return new ExpressDetailsComposite(composite, callback, mode, showVerify);
	}
	
	protected IServerModeUICallback callback;
	private ModifyListener nameModifyListener, remoteModifyListener, 
							passModifyListener, appModifyListener, deployProjectModifyListener;
	protected Text userText, passText, remoteText;
	protected Combo appNameCombo, deployProjectCombo;
	protected Button verifyButton;
	protected boolean showVerify;
	private Composite composite;
	private String user, pass, app, remote, deployProject;
	private IApplication fapplication;
	private IUser fuser;
	private String[] appListNames;
	private String error;
	private IServerWorkingCopy server;
	private String mode;
	public ExpressDetailsComposite(Composite fill, IServerModeUICallback callback, String mode, boolean showVerify) {
		this.callback = callback;
		this.server = callback.getServer();
		this.mode = mode;
		this.composite = fill;
		this.showVerify = showVerify;
		fillSection(fill);
		addListeners();
	}
	
	public Composite getComposite() {
		return composite;
	}
	
	public String getPassword() {
		return pass;
	}
	
	private void fillSection(Composite composite) {
		composite.setLayout(new GridLayout(2, false));
		Label userLabel = new Label(composite, SWT.NONE);
		userText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(userText);
		Label passLabel = new Label(composite, SWT.NONE);
		passText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(passText);
		
		if( mode.equals(ExpressServerUtils.EXPRESS_SOURCE_MODE) ) {
			Label appNameLabel = new Label(composite, SWT.NONE);
			appNameCombo = new Combo(composite, SWT.NONE);
			appNameLabel.setText("Application Name: " );
			String aName = ExpressServerUtils.getExpressApplicationName(server);
			if( aName != null ) appNameCombo.setText(aName);
			if( aName != null ) appNameCombo.setEnabled(false);
		} else {
			Label deployLocationLabel = new Label(composite, SWT.NONE);
			deployProjectCombo = new Combo(composite, SWT.NONE);
			deployLocationLabel.setText("Openshift Project: " );
			String[] projectNames = discoverOpenshiftProjects();
			deployProjectCombo.setItems(projectNames);
			String depLoc = ExpressServerUtils.getExpressDeployProject(server);
			if( depLoc != null ) deployProjectCombo.setText(depLoc);
			if( depLoc != null ) deployProjectCombo.setEnabled(false);
		}
		
		Label remoteLabel = new Label(composite, SWT.NONE);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(remoteText);
		
		
		// Text
		userLabel.setText("Username: ");
		passLabel.setText("Password: ");
		remoteLabel.setText("Remote: ");
		remoteText.setText(IOpenShiftWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT);
		
		String n = ExpressServerUtils.getExpressUsername(server);
		String p = ExpressServerUtils.getExpressPassword(server.getOriginal());
		String remote = ExpressServerUtils.getExpressRemoteName(server);
		if( n != null ) userText.setText(n);
		if( p != null ) passText.setText(p);
		if( remote != null ) remoteText.setText(remote);
		
		if( showVerify ) {
			verifyButton = new Button(composite, SWT.PUSH);
			verifyButton.setText("Verify...");
		}
	}
	
	private String[] discoverOpenshiftProjects() { 
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<String> names = new ArrayList<String>();
		for( int i = 0; i < projects.length; i++ ) {
			if( isOpenshiftProjectWithDeploymentsFolder(projects[i])) {
				names.add(projects[i].getName());
			}
		}
		return (String[]) names.toArray(new String[names.size()]);
	}
	
	private boolean isOpenshiftProjectWithDeploymentsFolder(IProject p) {
		// TODO add other criteria? 
		IFolder f = p.getFolder(".openshift");
		if( f != null && f.exists()) {
			return true;
		}
		return false;
	}
	
	private void addListeners() {
		nameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				user = userText.getText();
				callback.execute(new SetUserCommand(server));
			}
		};
		userText.addModifyListener(nameModifyListener);
		
		passModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				//pass = passText.getText();
				callback.execute(new SetPassCommand(server));
			}
		};
		passText.addModifyListener(passModifyListener);

		remoteModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				remote = remoteText.getText();
				callback.execute(new SetRemoteCommand(server));
			}
		};
		remoteText.addModifyListener(remoteModifyListener);

		if( appNameCombo != null ) {
			appModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					app = appNameCombo.getText();
					callback.execute(new SetApplicationCommand(server));
				}
			};
			appNameCombo.addModifyListener(appModifyListener);
		}		

		if( deployProjectCombo != null ) {
			deployProjectModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					deployProject = deployProjectCombo.getText();
					callback.execute(new SetDeployProjectCommand(server));
				}
			};
			deployProjectCombo.addModifyListener(deployProjectModifyListener);
		} 

		if( verifyButton != null ) {
			verifyButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					verifyPressed();
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
		}
	}
	
	private void verifyPressed() {
		final CredentialsWizardPageModel model = new CredentialsWizardPageModel();
		this.fapplication = null;
		this.fuser = null;
		this.appListNames = null;
		model.setPassword(pass);
		model.setRhLogin(user);
		verifyButton.setEnabled(false);
		final Runnable runnable = getVerifyingCredentialsJob(model);
		Job j = new Job("Verifying Credentials and Application") {
			protected IStatus run(IProgressMonitor monitor) {
				runnable.run();
				return Status.OK_STATUS;
			}
		};
		callback.executeLongRunning(j);
		postLongRunningValidate();
	}
	
	private void postLongRunningValidate() {
		if( appListNames == null ) {
			appListNames = new String[0];
		}
		if( appNameCombo != null ) {
			int index = Arrays.asList(appListNames).indexOf(app);
			appNameCombo.setItems(appListNames);
			if( index != -1 )
				appNameCombo.select(index);
		}
		callback.setErrorMessage(error);
		verifyButton.setEnabled(true);
	}


	public class SetUserCommand extends ServerWorkingCopyPropertyCommand {
		public SetUserCommand(IServerWorkingCopy server) {
			super(server, Messages.EditorChangeUsernameCommandName, userText, userText.getText(), 
					ExpressServerUtils.ATTRIBUTE_USERNAME, nameModifyListener);
		}
	}

	public class SetRemoteCommand extends ServerWorkingCopyPropertyCommand {
		public SetRemoteCommand(IServerWorkingCopy server) {
			super(server, "Change Remote Name", remoteText, remoteText.getText(), 
					ExpressServerUtils.ATTRIBUTE_REMOTE_NAME, remoteModifyListener);
		}
	}

	public class SetApplicationCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetApplicationCommand(IServerWorkingCopy server) {
			super(server, "Change Application Name", appNameCombo, appNameCombo.getText(), 
					ExpressServerUtils.ATTRIBUTE_APPLICATION_NAME, appModifyListener);
		}
	}

	public class SetDeployProjectCommand extends ServerWorkingCopyPropertyComboCommand {
		public SetDeployProjectCommand(IServerWorkingCopy server) {
			super(server, "Change Deployment Project", appNameCombo, deployProjectCombo.getText(), 
					ExpressServerUtils.ATTRIBUTE_DEPLOY_PROJECT, deployProjectModifyListener);
		}
	}

	public class SetPassCommand extends ServerWorkingCopyPropertyCommand {
		public SetPassCommand(IServerWorkingCopy server) {
			super(server, Messages.EditorChangePasswordCommandName, passText, passText.getText(), 
					IJBossToolingConstants.SERVER_PASSWORD, passModifyListener);
			oldVal = passText.getText();
		}
		
		public void execute() {
			pass = newVal;
		}
		
		public void undo() {
			pass = oldVal;
			text.removeModifyListener(listener);
			text.setText(oldVal);
			text.addModifyListener(listener);
		}
	}

	private Runnable getVerifyingCredentialsJob(final CredentialsWizardPageModel model) {
		return new Runnable() {
			public void run() {
				final IStatus s = model.validateCredentials();
				String error = null;
				if( !s.isOK() ) {
					error = "Credentials Failed";
				} else {
					
					if( mode.equals(ExpressServerUtils.EXPRESS_SOURCE_MODE) ) {
						verifyApplicationSourceMode(model);
					} else {
						verifyApplicationBinaryMode(model);
					}
				}
				ExpressDetailsComposite.this.error = error;
			}
		};
	}
	
	private IApplication findApplicationForProject(IProject p, List<IApplication> applications) 
			throws OpenShiftException, CoreException {
		List<URIish> uris = EGitUtils.getRemoteURIs(p);
		Iterator<IApplication> i = applications.iterator();
		while(i.hasNext()) {
			IApplication a = i.next();
			String gitUri = a.getGitUri();
			Iterator<URIish> j = uris.iterator();
			while(j.hasNext()) {
				String projUri = j.next().toPrivateString();
				if( projUri.equals(gitUri)) {
					return a;
				}
			}
		}
		return null;
	}
	
	private void verifyApplicationBinaryMode(CredentialsWizardPageModel model) {
		System.out.println(deployProject);
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);
		try {
			final List<IApplication> allApps = model.getUser().getApplications();
			fapplication = findApplicationForProject(p, allApps);
			fuser = model.getUser();
			
			if( fapplication == null ) {
				error = "Application for project \"" + p.getName() + "\" not found";
			}
			IServerWorkingCopy wc = callback.getServer(); 
			ExpressServerUtils.fillServerWithOpenShiftDetails(wc, fapplication, fuser,
					mode, remote);
			wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
			wc.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
			wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY, p.getFolder("deployments").getLocation().toString());
			wc.setAttribute(IDeployableServer.TEMP_DEPLOY_DIRECTORY, p.getFolder("deployments").getLocation().toString());
		} catch( OpenShiftException ce ) {
			
		} catch( CoreException ce) {
			
		}
	}
	
	private void verifyApplicationSourceMode(CredentialsWizardPageModel model) {
		// now check the app name and cartridge
		String[] appNames = new String[]{};
		try {
			final List<IApplication> allApps = model.getUser().getApplications();
			appNames = getAppNamesAsStrings(allApps);
			int index = Arrays.asList(appNames).indexOf(app);
			IApplication application = index == -1 ? null : allApps.get(index);
			ExpressDetailsComposite.this.appListNames = appNames;
			if( application == null ) {
				error = "Application " + app + " not found";
			} else {
				// Fill with new data
				try {
					ExpressDetailsComposite.this.fapplication = application;
					ExpressDetailsComposite.this.fuser = model.getUser();
					
					// update the values 
					IServerWorkingCopy wc = callback.getServer(); 
					ExpressServerUtils.fillServerWithOpenShiftDetails(wc, application, fuser,
							mode, remote);
				} catch(CoreException ce) {
					// TODO FIX HANDLE
				}
			}
		} catch(OpenShiftException ose) {
			error = "Application \"" + app + "\" not found: " + ose.getMessage();
		}
	}

	
	
	private String[] getAppNamesAsStrings(List<IApplication> allApps) {
		String[] appNames = new String[allApps.size()];
		for( int i = 0; i < allApps.size(); i++ ) {
			appNames[i] = allApps.get(i).getName();
		}
		return appNames;
	}


}
