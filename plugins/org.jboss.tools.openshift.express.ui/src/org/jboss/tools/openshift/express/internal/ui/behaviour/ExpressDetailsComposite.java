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

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.core.console.UserModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.ConnectToOpenShiftWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPageModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;

/**
 * @author Rob Stryker
 */
public class ExpressDetailsComposite {
	// How to display errors, set attributes, etc
	protected IServerModeUICallback callback;
	
	// Widgets 
	private ModifyListener nameModifyListener, remoteModifyListener,
			appModifyListener, deployProjectModifyListener, deployDestinationModifyListener;
	private ModifyListener passModifyListener;
	private Composite composite;
	private Link importLink;
	protected Text userText, remoteText;
	protected Text passText;
	protected Text deployFolderText;
	protected Combo appNameCombo, deployProjectCombo;
	protected Button verifyButton,  browseDestButton, rememberPasswordCheckBox;
	protected boolean showVerify, showImportLink;
	
	// Data / Model 
	private boolean rememberPassword = true;
	private String user, pass, app, remote, deployProject, deployFolder;
	private IApplication fapplication;
	private UserDelegate fuser;
	private IDomain fdomain;
	private List<IApplication> appList;
	private String[] appListNames;
	private IServerWorkingCopy server;
	private HashMap<IApplication, IProject[]> projectsPerApp = new HashMap<IApplication, IProject[]>();
	private boolean credentialsFailed = false;
	
	public ExpressDetailsComposite(Composite fill, IServerModeUICallback callback, boolean showVerify) {
		this.callback = callback;
		this.server = callback.getServer();
		this.composite = fill;
		this.showVerify = showVerify;
		this.showImportLink = showVerify;
		try {
			initModel();
			createWidgets(fill);
			fillWidgets();
			addListeners();
			quickValidate();
		} catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Composite getComposite() {
		return composite;
	}

	private void initModel() {
		String nameFromExistingServer = ExpressServerUtils.getExpressUsername(server);
		if (nameFromExistingServer == null) {
			initModelNewServerWizard();
			return;
		}

		this.user = nameFromExistingServer;
		this.fuser = UserModel.getDefault().findUser(this.user);
		this.app = ExpressServerUtils.getExpressApplicationName(server);
		this.pass = UserModel.getDefault().getPasswordFromSecureStorage(this.user);
		this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
		this.deployFolder = ExpressServerUtils.getExpressDeployFolder(server);
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
	}

	private void initModelNewServerWizard() {
		// We're in a new server wizard.
		UserDelegate tmpUser = (UserDelegate) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_USER);
		IApplication app = (IApplication) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_SELECTED_APP);
		
		if( tmpUser != null && app != null ) {
			// started from express console with a user and an app
			try {
				this.app = app.getName();
				updateModelForNewUser(tmpUser);
				quickValidate();
				showVerify = false;
				IProject[] p = projectsPerApp.get(app);
				showImportLink = p == null || p.length == 0;
			} catch( OpenShiftException ose ) {
				// ignore, allow appList and appListNames to be null / empty
			} catch( SocketTimeoutException ste) {
				// ignore, allow appList and appListNames to be null / empty
			}
		} else {
			// we may or may not have a user, clearly no app
			this.fuser = tmpUser == null ? UserModel.getDefault().getRecentUser() : tmpUser;
			this.user = fuser == null ? null : fuser.getRhlogin();
		}
		
		this.pass = this.user == null ? null : UserModel.getDefault().getPasswordFromSecureStorage(this.user);
		this.deployFolder = ExpressServerUtils.getExpressDeployFolder(server);
		this.deployFolder = this.deployFolder == null ? ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT : this.deployFolder;
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
		this.remote = this.remote == null ? ExpressServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT : this.remote;
	}
	
	/* Set widgets initial values */
	private void fillWidgets() {
		if (user != null) {
			userText.setText(user);
			userText.setEnabled(showVerify);
		}
		if( showVerify ) {
			if (pass != null) {
				passText.setText(pass);
				passText.setEnabled(fapplication == null);
			}
			rememberPassword = pass != null && !"".equals(pass);
			rememberPasswordCheckBox.setSelection(rememberPassword);
		}
		if (remote != null)
			remoteText.setText(remote);
		if (appListNames != null)
			appNameCombo.setItems(appListNames);
		if (app != null) {
			appNameCombo.setEnabled(false);
			appNameCombo.setItems(new String[]{app});
			appNameCombo.select(0);
			if( fapplication != null ) {
				resetDeployProjectCombo();
			} else {
				this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
				if( deployProject != null ) {
					this.deployProjectCombo.setItems(new String[]{deployProject});
					this.deployProjectCombo.select(0);
					this.deployProjectCombo.setEnabled(false);
				} else {
					this.browseDestButton.setEnabled(false);
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
			browseDestButton.setEnabled(true);
		} else {
			browseDestButton.setEnabled(false);
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
			verifyButton = new Button(composite, SWT.PUSH);
			verifyButton.setText("Verify...");
			
			// Add label to check for password remember
			rememberPasswordCheckBox = new Button(composite, SWT.CHECK);
			rememberPasswordCheckBox.setText(OpenshiftUIMessages.OpenshiftWizardSavePassword);
		}
		
		Label appNameLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameCombo = new Combo(composite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameCombo);
		appNameLabel.setText("Application Name: ");
		
		Label deployLocationLabel = new Label(composite, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);
		deployLocationLabel.setText("Deploy Project: " );
		
		if( showImportLink ) {
			importLink = new Link(composite, SWT.DEFAULT);
			importLink.setText("<a>Import this application</a>"); //$NON-NLS-1$
			//  if we show verify, start import link disabled (wait for verify pressed to enable)
			//  Otherwise, not showing verify means we're inside new wizard fragment with no suitable projects
			importLink.setEnabled(!showVerify);
			GridData gd = GridDataFactory.fillDefaults().span(2, 1).create();
			importLink.setLayoutData(gd);
		}
		
		Label remoteLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
		
		Group projectSettings = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).span(2,1).applyTo(projectSettings);
		projectSettings.setLayout(new GridLayout(2, false));
		
		Label zipDestLabel = new Label(projectSettings, SWT.NONE);
		zipDestLabel.setText("Output Directory: ");
		
		Composite zipDestComposite = new Composite(projectSettings, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
		zipDestComposite.setLayout(new FormLayout());
		browseDestButton = new Button(zipDestComposite, SWT.PUSH);
		browseDestButton.setText("Browse...");
		browseDestButton.setLayoutData(UIUtil.createFormData2(0,5,100,-5,null,0,100,0));
		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0,5,100,-5,0,0,browseDestButton,-5));

		// Text
		projectSettings.setText("Project Settings");
		userLabel.setText("Username: ");
		if( passLabel != null ) passLabel.setText("Password: ");
		remoteLabel.setText("Remote: ");
	}

	private void addListeners() {
		nameModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				user = userText.getText();
				String storedPass = UserModel.getDefault().getPasswordFromSecureStorage(user);
				if (storedPass != null && !storedPass.equals(""))
					passText.setText(storedPass);
			}
		};
		userText.addModifyListener(nameModifyListener);

		passModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				pass = passText.getText();
			}
		};
		
		if( showVerify ) 
			passText.addModifyListener(passModifyListener);

		remoteModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				remote = remoteText.getText();
			}
		};
		remoteText.addModifyListener(remoteModifyListener);

		if (appNameCombo != null) {
			appModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					app = appNameCombo.getText();
					int ind = appNameCombo.indexOf(app);
					if( ind != -1 ) {
						fapplication = appList.get(ind);
					}
					resetDeployProjectCombo();
					enableImportLink();
					quickValidate();
				}
			};
			appNameCombo.addModifyListener(appModifyListener);
		}

		if (deployProjectCombo != null) {
			deployProjectModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					deployProject = deployProjectCombo.getText();
				}
			};
			deployProjectCombo.addModifyListener(deployProjectModifyListener);
		}
		deployDestinationModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deployFolder = deployFolderText.getText();
			}
		};
		deployFolderText.addModifyListener(deployDestinationModifyListener);
		
		browseDestButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browsePressed();
			}
		});

		
		if (showImportLink ) {
			importLink.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					OpenShiftExpressApplicationWizard wizard = 
							new ImportOpenShiftExpressApplicationWizard(fuser, null, fapplication, true);
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
		}
		if (showVerify) {
			verifyButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					verifyPressed();
				}
			});
			rememberPasswordCheckBox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					rememberPassword = rememberPasswordCheckBox.getSelection();
				}
			});
		}
		
		deployProjectCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deployProjectChanged();
			}
		});
	}

	private void deployProjectChanged() {
		int i = deployProjectCombo.getSelectionIndex();
		if( i != -1 ) {
			IProject depProj = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);
			if( depProj != null && depProj.isAccessible() ) {
				String depFolder = ExpressServerUtils.getProjectAttribute(depProj, 
						ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, null); 
						//ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
				if( depFolder == null )
					deployFolderText.setText(ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
				deployFolderText.setEnabled(depFolder == null);
				browseDestButton.setEnabled(depFolder == null);
			}
		}
	}
	
	private void browsePressed() {
		IFolder f = chooseFolder();
		if( f != null ) {
			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
		}
	}
	
	private IFolder chooseFolder() {
		if( this.deployProject == null )
			return null;
		
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
		this.fapplication = null;
		this.fuser = null;
		this.appListNames = null;
		verifyButton.setEnabled(false);
		final Runnable runnable = getVerifyingCredentialsJob();
		Job j = new Job("Verifying Credentials and Application") {
			protected IStatus run(IProgressMonitor monitor) {
				runnable.run();
				return Status.OK_STATUS;
			}
		};
		callback.executeLongRunning(j);
		postVerifyUpdateWidgets();
		quickValidate();
	}

	private void postVerifyUpdateWidgets() {
		importLink.setEnabled(false);
		verifyButton.setEnabled(true);
		if (appNameCombo != null) {
			appNameCombo.setItems(appListNames);
			int index = Arrays.asList(appListNames).indexOf(app);
			if (index != -1)
				appNameCombo.select(index);
			else if( (app == null || "".equals(app)) && appListNames.length > 0) {
				int select = 0;
				for( int i = 0; i < appList.size(); i++ ) {
					IProject[] p = projectsPerApp.get(appList.get(i));
					if( p != null && p.length > 0 ) {
						select = i;
						break;
					}
				}
				appNameCombo.select(select);
			}
		}
		enableImportLink();
		resetDeployProjectCombo();
	}
	
	private void enableImportLink() {
		IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
		importLink.setEnabled(p == null || p.length == 0);
	}

	private void quickValidate() {
		if( !showVerify )
			return;
		callback.setErrorMessage(getErrorString());
	}

	public String getErrorString() {
		String error = null;
		if( credentialsFailed ) {
			error = "Credentials Failed";
		} else if( appList == null ) {
			error = "Please click \"verify\" to test your credentials.";
		} else if( fdomain == null ) {
			error = "Your OpenShift account has not been configured with a domain.";
		} else if( app == null || app.equals("")) {
			error = "Please select an application from the combo below.";
		} else {
			IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
			if (p == null || p.length == 0) {
				error = "Your workspace does not have a project corresponding to " + app + ". Please import one.";
			}
		}
		return error;
	}

	private Runnable getVerifyingCredentialsJob() {
		final ConnectToOpenShiftWizardModel inner = new ConnectToOpenShiftWizardModel() {
			public UserDelegate setUser(UserDelegate user) {
				created = user;
				return user;
			}
		};
		final CredentialsWizardPageModel model = new CredentialsWizardPageModel(inner);
		model.setPassword(pass);
		model.setRhLogin(user);
		model.setRememberPassword(false);
		return new Runnable() {
			public void run() {
				final IStatus s = model.validateCredentials();
				if (!s.isOK()) {
					credentialsFailed = true;
				} else {
					credentialsFailed = false;
					try {
						updateModelForNewUser(inner.getUser());
					} catch(NotFoundOpenShiftException nose) {
						// Ignore this. It will be handled later
					} catch(OpenShiftException ose) {
					} catch(SocketTimeoutException ose) {
					}
				}
			}
		};
	}

	private void updateModelForNewUser(UserDelegate user) throws OpenShiftException, SocketTimeoutException {
		
		// Updating the model, some long-running 
		projectsPerApp.clear();
		try {
			// IF we load the applications first, domain gets loaded automatically
			this.appList = user.getApplications();
			fdomain = user.getDefaultDomain();
		} catch(NotFoundOpenShiftException nfose) {
			// Credentials work, but no domain, so no applications either
			this.appList = new ArrayList<IApplication>();
			fdomain = null;
		}
		String[] appNames = getAppNamesAsStrings(appList);
		int index = Arrays.asList(appNames).indexOf(app);
		IApplication application = index == -1 ? null : appList.get(index);
		this.appListNames = appNames == null ? new String[0] : appNames;
		this.fapplication = application;
		this.fuser = user;
		this.user = fuser.getRhlogin();
		
		for( int i = 0; i < appList.size(); i++ ) {
			projectsPerApp.put(appList.get(i), ExpressServerUtils.findProjectsForApplication(appList.get(i)));
		}
		
		IProject[] possibleProjects = projectsPerApp.get(fapplication);
		this.deployProject = possibleProjects == null || possibleProjects.length == 0 ? null : possibleProjects[0].getName();
		
		fillServerWithDetails();
	}
	
	
	public void finish(IProgressMonitor monitor) throws CoreException {
		try {
			UserModel.getDefault().addUser(fuser);
			if( rememberPassword ) {
				UserModel.getDefault().setPasswordInSecureStorage(fuser.getRhlogin(), fuser.getPassword());
			} else {
				UserModel.getDefault().clearPasswordInSecureStorage(fuser.getRhlogin());
			}
			fillServerWithDetails();
			updateProjectSettings();
		} catch(OpenShiftException ose) {
			throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, ose.getMessage(), ose));
		}
	}
	private void fillServerWithDetails() throws OpenShiftException {
		// Fill the server working copy
		// update the values
		IServerWorkingCopy wc = callback.getServer();
		String host = fapplication == null ? null : fapplication.getApplicationUrl();
		ExpressServerUtils.fillServerWithOpenShiftDetails(wc, host, deployProject,remote);
	}
	
	private void updateProjectSettings() {
		IProject depProj = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);

		String projRemote = ExpressServerUtils.getProjectAttribute(depProj, 
				ExpressServerUtils.SETTING_REMOTE_NAME, null);
		String projDepFolder = ExpressServerUtils.getProjectAttribute(depProj, 
				ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, null); 
		if( projRemote == null && projDepFolder == null ) {
			ExpressServerUtils.updateOpenshiftProjectSettings(
					depProj, fapplication, fuser, remote, deployFolder);
		}
	}

	private String[] getAppNamesAsStrings(List<IApplication> allApps) {
		String[] appNames = new String[allApps.size()];
		for (int i = 0; i < allApps.size(); i++) {
			appNames[i] = allApps.get(i).getName();
		}
		return appNames;
	}
}
