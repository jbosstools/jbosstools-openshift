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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.core.server.IDeployableServer;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPageModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.IOpenShiftWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

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
							appModifyListener, deployProjectModifyListener;
	private ModifyListener passModifyListener;
	private Link importLink;
	protected Text userText, remoteText;
	protected Text passText;
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
		String[] appNames = null;
		if( n == null ) {
			// We're in a new server wizard
			IUser user = UserModel.getDefault().getRecentUser();
			if( user == null && UserModel.getDefault().getUsers().length > 0 ) {
				user = UserModel.getDefault().getUsers()[0];
			}
			if( user != null )
				try {
					n = user.getRhlogin();
					List<IApplication> allApps = user.getApplications();
					appNames = getAppNamesAsStrings(allApps);
				} catch(Exception e) { /* ignore */ }
		}
		String p = UserModel.getDefault().getPasswordFromSecureStorage(n);
		String remote = ExpressServerUtils.getExpressRemoteName(server);
		if( n != null ) userText.setText(n);
		if( p != null ) passText.setText(p);
		if( remote != null ) remoteText.setText(remote);
		
		if( appNames != null ) {
			appListNames = appNames;
			appNameCombo.setItems(appListNames);
		}
		
		if( showVerify ) {
			importLink = new Link(composite, SWT.DEFAULT);
			importLink.setText("<a>Import this application</a>"); //$NON-NLS-1$
			importLink.setEnabled(false);
			GridData gd = GridDataFactory.fillDefaults().span(2, 1).create();
			importLink.setLayoutData(gd);
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
				String storedPass = UserModel.getDefault().getPasswordFromSecureStorage(user);
				if( storedPass != null && !storedPass.equals(""))
					passText.setText(storedPass);
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
			importLink.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					OpenShiftExpressApplicationWizard wizard = new ImportOpenShiftExpressApplicationWizard();
					wizard.setInitialUser(fuser);
					wizard.setSelectedApplication(fapplication);
					WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
					dialog.create();
					dialog.open();
				}
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

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
		importLink.setEnabled(true);
		if( appListNames == null ) {
			appListNames = new String[0];
		}
		if( appNameCombo != null ) {
			int index = Arrays.asList(appListNames).indexOf(app);
			appNameCombo.setItems(appListNames);
			if( index != -1 )
				appNameCombo.select(index);
		}
		if( error == null ) {
			IProject p = ExpressServerUtils.findProjectForApplication(fapplication);
			if( p == null ) {
				error = "Your workspace does not have a project corresponding to " + app +". Please import one.";
				importLink.setEnabled(true);
			}
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
					null, passModifyListener);
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
				if( !s.isOK() ) {
					ExpressDetailsComposite.this.error = "Credentials Failed";
				} else {
					
					if( mode.equals(ExpressServerUtils.EXPRESS_SOURCE_MODE) ) {
						verifyApplicationSourceMode(model);
					} else {
						verifyApplicationBinaryMode(model);
					}
				}
			}
		};
	}
	

	
	private void verifyApplicationBinaryMode(CredentialsWizardPageModel model) {
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);
		try {
			fuser = OpenShiftUIActivator.getDefault().getUser();
			final List<IApplication> allApps = fuser.getApplications();
			fapplication = ExpressServerUtils.findApplicationForProject(p, allApps);
			
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
		error = null;
		// now check the app name and cartridge
		String[] appNames = new String[]{};
		try {
			IUser user = OpenShiftUIActivator.getDefault().getUser();
			final List<IApplication> allApps = user.getApplications();
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
					ExpressDetailsComposite.this.fuser = user;
					IProject p = ExpressServerUtils.findProjectForApplication(fapplication);
					
					// update the values 
					IServerWorkingCopy wc = callback.getServer(); 
					ExpressServerUtils.fillServerWithOpenShiftDetails(wc, application, fuser,
							mode, remote);
					wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY_TYPE, IDeployableServer.DEPLOY_CUSTOM);
					wc.setAttribute(IDeployableServer.ZIP_DEPLOYMENTS_PREF, true);
					wc.setAttribute(IDeployableServer.DEPLOY_DIRECTORY, p.getFolder("deployments").getLocation().toString());
					wc.setAttribute(IDeployableServer.TEMP_DEPLOY_DIRECTORY, p.getFolder("deployments").getLocation().toString());
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

	public String getUsername() {
		return user;
	}
	
	public String getPassword() {
		return pass;
	}
	
	public String getApplicationName() {
		return app;
	}

	public IUser getUser() {
		return fuser;
	}
	
	public IApplication getApplication() {
		return fapplication;
	}
	
	public String getRemote() {
		return remote;
	}
}
