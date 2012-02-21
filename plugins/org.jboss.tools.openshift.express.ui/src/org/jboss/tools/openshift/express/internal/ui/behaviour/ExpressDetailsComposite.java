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

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.internal.handlers.ResetPerspectiveHandler;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyComboCommand;
import org.jboss.ide.eclipse.as.ui.editor.ServerWorkingCopyPropertyCommand;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.ConnectToOpenShiftWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPageModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.IOpenShiftExpressWizardModel;
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
			appModifyListener, deployProjectModifyListener, deployDestinationModifyListener;
	private ModifyListener passModifyListener;
	private Link importLink;
	protected Text userText, remoteText;
	protected Text passText;
	protected Text deployFolderText;
	protected Combo appNameCombo, deployProjectCombo;
	protected Button verifyButton,  browseDestButton;
	protected boolean showVerify;
	private Composite composite;
	private String user, pass, app, remote, deployProject, deployFolder;
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
		initModel();
		createWidgets(fill);
		fillWidgets();
		addListeners();
	}

	public Composite getComposite() {
		return composite;
	}

	// We already have an initial user, so username and pw can be frozen
	private boolean createServerHasInitialUser = false;
	
	private void initModel() {
		String nameFromExistingServer = ExpressServerUtils.getExpressUsername(server);
		if (nameFromExistingServer == null) {
			// We're in a new server wizard.
			// First, check if the taskmodel has data
			IUser tmpUser = (IUser) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_USER);
			createServerHasInitialUser = tmpUser != null;
			if (tmpUser == null) {
				// If not, use recent user
				tmpUser = UserModel.getDefault().getRecentUser();
			}
			if (tmpUser == null && UserModel.getDefault().getUsers().length > 0) {
				tmpUser = UserModel.getDefault().getUsers()[0];
			}
			if (tmpUser != null) {
				try {
					this.fuser = tmpUser;
					this.user = tmpUser.getRhlogin();
					if( createServerHasInitialUser ) {
						List<IApplication> allApps = tmpUser.getApplications();
						this.appListNames = getAppNamesAsStrings(allApps);
					}
				} catch (Exception e) { 
					/* TODO */
				}
			}
		} else {
			this.user = nameFromExistingServer;
		}
		IApplication app = (IApplication) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_SELECTED_APP);
		if (app != null) {
			this.fapplication = app;
			this.app = app.getName();
		} else {
			this.app = ExpressServerUtils.getExpressApplicationName(server);
		}

		this.pass = UserModel.getDefault().getPasswordFromSecureStorage(this.user);
		this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
		this.deployFolder = ExpressServerUtils.getExpressDeployFolder(server);
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
		this.remote = this.remote == null ? IOpenShiftExpressWizardModel.NEW_PROJECT_REMOTE_NAME_DEFAULT : this.remote;
	}

	/* Set widgets initial values */
	private void fillWidgets() {
		if (user != null) {
			userText.setText(user);
			userText.setEnabled(!createServerHasInitialUser);
		}
		if (showVerify && pass != null) {
			passText.setText(pass);
			passText.setEnabled(!createServerHasInitialUser);
		}
		if (remote != null)
			remoteText.setText(remote);
		if (appListNames != null)
			appNameCombo.setItems(appListNames);
		app = ExpressServerUtils.getExpressApplicationName(server);
		app = app != null ? app : fapplication == null ? null : fapplication.getName();
		if (app != null) {
			appNameCombo.setText(app);
			appNameCombo.setEnabled(false);
			int ind = appNameCombo.indexOf(app);
			if (ind != -1) {
				appNameCombo.select(ind);
			}

			if( fapplication != null ) {
				resetDeployProjectCombo();
			} else {
				this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
				if( deployProject != null ) {
					this.deployProjectCombo.setItems(new String[]{deployProject});
					this.deployProjectCombo.select(0);
					this.deployProjectCombo.setEnabled(false);
				}
			}
		}
		
		deployFolderText.setText(deployFolder);
		remoteText.setText(remote);
	}

	private void resetDeployProjectCombo() {
		IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
		String[] names = p == null ? new String[0] : new String[p.length];
		for( int i = 0; i < names.length; i++ ) {
			names[i] = p[i].getName();
		}
		this.deployProjectCombo.setItems(names);
		if( names.length > 0 ) {
			deployProjectCombo.select(0);
			this.deployProject = names[0];
		}
	}
	
	private void createWidgets(Composite composite) {
		composite.setLayout(new GridLayout(2, false));
		Label userLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(userLabel);
		userText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(userText);
		Label passLabel = null;
		if( showVerify ) {
			passLabel = new Label(composite, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(passLabel);
			passText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(passText);
		}
		Label appNameLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameCombo = new Combo(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameCombo);
		appNameLabel.setText("Application Name: ");
		
		Label deployLocationLabel = new Label(composite, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);
		deployLocationLabel.setText("Deploy Project: " );
		
		Label zipDestLabel = new Label(composite, SWT.NONE);
		zipDestLabel.setText("Output Directory: ");
		
		Composite zipDestComposite = new Composite(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
		zipDestComposite.setLayout(new FormLayout());
		browseDestButton = new Button(zipDestComposite, SWT.DEFAULT);
		browseDestButton.setText("Browse...");
		browseDestButton.setLayoutData(UIUtil.createFormData2(0,5,100,-5,null,0,100,-5));
		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0,5,100,-5,0,5,browseDestButton,-5));
		
		
		
		Label remoteLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);

		// Text
		userLabel.setText("Username: ");
		if( passLabel != null ) passLabel.setText("Password: ");
		remoteLabel.setText("Remote: ");

		if (showVerify) {
			importLink = new Link(composite, SWT.DEFAULT);
			importLink.setText("<a>Import this application</a>"); //$NON-NLS-1$
			importLink.setEnabled(false);
			GridData gd = GridDataFactory.fillDefaults().span(2, 1).create();
			importLink.setLayoutData(gd);
			verifyButton = new Button(composite, SWT.PUSH);
			verifyButton.setText("Verify...");
		}
	}

	private void addListeners() {
		nameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				user = userText.getText();
				String storedPass = UserModel.getDefault().getPasswordFromSecureStorage(user);
				if (storedPass != null && !storedPass.equals(""))
					passText.setText(storedPass);
				callback.execute(new SetUserCommand(server));
			}
		};
		userText.addModifyListener(nameModifyListener);

		passModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// pass = passText.getText();
				callback.execute(new SetPassCommand(server));
			}
		};
		if( showVerify ) passText.addModifyListener(passModifyListener);

		remoteModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				remote = remoteText.getText();
				callback.execute(new SetRemoteCommand(server));
			}
		};
		remoteText.addModifyListener(remoteModifyListener);

		if (appNameCombo != null) {
			appModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					app = appNameCombo.getText();
					callback.execute(new SetApplicationCommand(server));
					resetDeployProjectCombo();
				}
			};
			appNameCombo.addModifyListener(appModifyListener);
		}

		if (deployProjectCombo != null) {
			deployProjectModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					deployProject = deployProjectCombo.getText();
					callback.execute(new SetDeployProjectCommand(server));
				}
			};
			deployProjectCombo.addModifyListener(deployProjectModifyListener);
		}
		deployDestinationModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deployFolder = deployFolderText.getText();
				callback.execute(new SetDeployFolderCommand(server));
			}
		};
		deployFolderText.addModifyListener(deployDestinationModifyListener);
		
		browseDestButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browsePressed();
			}
		});

		
		if (verifyButton != null) {
			importLink.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					OpenShiftExpressApplicationWizard wizard = new ImportOpenShiftExpressApplicationWizard();
					wizard.setInitialUser(fuser);
					wizard.setSelectedApplication(fapplication);
					WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
					int oldServerCount = ServerCore.getServers().length;
					dialog.create();
					dialog.open();
					if( ServerCore.getServers().length > oldServerCount ) {
						// Cancel this wizard, a server has already been created
						// This is reeeaally ugly
						IWizardHandle handle = ((DeploymentTypeUIUtil.NewServerWizardBehaviourCallback)callback).getHandle();
						IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
						((WizardDialog)container).close();
					}
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

	private void browsePressed() {
		IFolder f = chooseFolder();
		if( f != null ) {
			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
		}
	}
	
	private IFolder chooseFolder() {
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(this.deployProject);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), lp, cp);
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(p);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		
		IResource res= p.findMember(new Path(this.deployFolder));
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == Window.OK) {
			return (IFolder) dialog.getFirstResult();
		}
		return null;
	}
	
	private void verifyPressed() {
		final CredentialsWizardPageModel model = new CredentialsWizardPageModel(new ConnectToOpenShiftWizardModel());
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
		importLink.setEnabled(false);
		if (appListNames == null) {
			appListNames = new String[0];
		}
		if (appNameCombo != null) {
			int index = Arrays.asList(appListNames).indexOf(app);
			appNameCombo.setItems(appListNames);
			if (index != -1)
				appNameCombo.select(index);
		}
		if (error == null) {
			IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
			if (p == null || p.length == 0) {
				error = "Your workspace does not have a project corresponding to " + app + ". Please import one.";
				importLink.setEnabled(true);
			}
			resetDeployProjectCombo();
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

	public class SetDeployFolderCommand extends ServerWorkingCopyPropertyCommand {
		public SetDeployFolderCommand(IServerWorkingCopy server) {
			super(server, "Change Deployment Folder", deployFolderText, deployFolderText.getText(),
					ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_NAME, deployDestinationModifyListener);
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
				if (!s.isOK()) {
					ExpressDetailsComposite.this.error = "Credentials Failed";
				} else {
					verifyApplicationSourceMode(model);
				}
			}
		};
	}

	private void verifyApplicationSourceMode(CredentialsWizardPageModel model) {
		error = null;
		// now check the app name and cartridge
		String[] appNames = new String[] {};
		try {
			IUser user = UserModel.getDefault().getRecentUser();
			final List<IApplication> allApps = user.getApplications();
			appNames = getAppNamesAsStrings(allApps);
			int index = Arrays.asList(appNames).indexOf(app);
			IApplication application = index == -1 ? null : allApps.get(index);
			ExpressDetailsComposite.this.appListNames = appNames;
			if (application == null) {
				error = "Application " + app + " not found. Please select one from the combo box.";
			} else {
				// Fill with new data
				try {
					ExpressDetailsComposite.this.fapplication = application;
					ExpressDetailsComposite.this.fuser = user;
					if( deployProject != null && !deployProject.equals("")) {
						// update the values
						IServerWorkingCopy wc = callback.getServer();
						ExpressServerUtils.fillServerWithOpenShiftDetails(wc, application, 
								fuser, mode, deployProject, deployFolder, remote);
					}
				} catch (CoreException ce) {
					// TODO FIX HANDLE
				}
			}
		} catch (OpenShiftException ose) {
			error = "Application \"" + app + "\" not found: " + ose.getMessage();
		}
	}

	private String[] getAppNamesAsStrings(List<IApplication> allApps) {
		String[] appNames = new String[allApps.size()];
		for (int i = 0; i < allApps.size(); i++) {
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
