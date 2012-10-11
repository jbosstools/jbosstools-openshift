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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModel;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.explorer.ConnectToOpenShiftWizard;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUpdatingJob;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftExpressApplicationWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 */
public class ExpressDetailsComposite {
	// How to display errors, set attributes, etc
	protected IServerModeUICallback callback;

	// Widgets
	private ModifyListener remoteModifyListener,
			appModifyListener, deployProjectModifyListener, deployDestinationModifyListener;
	private Composite composite;
	private Link importLink;
	private ComboViewer connectionComboViewer;
	protected Text remoteText;
	protected Text deployFolderText;
	protected Combo appNameCombo, deployProjectCombo;
	protected Button browseDestButton;
	protected boolean showVerify, showImportLink;

	// Data / Model
	private String connectionUrl, app, remote, deployProject, deployFolder;
	private IApplication fapplication;
	private Connection connection;
	private IDomain fdomain;
	private List<IApplication> appList;
	private String[] appListNames = new String[0];
	private IServerWorkingCopy server;
	private HashMap<IApplication, IProject[]> projectsPerApp = new HashMap<IApplication, IProject[]>();

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
			updateErrorMessage();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Composite getComposite() {
		return composite;
	}

	private void initModel() {
		String connectionUrl = ExpressServerUtils.getExpressConnectionUrl(server);
		if (ConnectionUtils.getDefaultHostUrl().equals(connectionUrl)) {
			initModelNewServerWizard();
			return;
		}
		
		this.connectionUrl = connectionUrl;
		this.connection = ConnectionsModel.getDefault().getConnectionByUrl(connectionUrl);
		this.app = ExpressServerUtils.getExpressApplicationName(server);
		this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
		this.deployFolder = ExpressServerUtils.getExpressDeployFolder(server);
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
	}

	private void initModelNewServerWizard() {
		// We're in a new server wizard.
		Connection tmpConnection = (Connection) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_CONNECTION);
		IApplication app = (IApplication) callback.getAttribute(ExpressServerUtils.TASK_WIZARD_ATTR_SELECTED_APP);

		if (tmpConnection != null && app != null) {
			// started from express console with a user and an app
			this.app = app.getName();
			updateModel(tmpConnection);
			updateErrorMessage();
			showVerify = false;
			IProject[] p = projectsPerApp.get(app);
			showImportLink = p == null || p.length == 0;
		} else {
			// we may or may not have a user, clearly no app
			this.connection = tmpConnection == null ? ConnectionsModel.getDefault().getRecentConnection()
					: tmpConnection;
			updateModel(connection);
		}

		this.deployFolder = ExpressServerUtils.getExpressDeployFolder(server);
		this.deployFolder = this.deployFolder == null ? ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT
				: this.deployFolder;
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
		this.remote = this.remote == null ? ExpressServerUtils.ATTRIBUTE_REMOTE_NAME_DEFAULT : this.remote;
	}

	/* Set widgets initial values */
	private void fillWidgets() {
		connectionComboViewer.setInput(ConnectionsModel.getDefault().getConnections());
		if (connection != null) {
			selectComboConnection(connection);
			connectionComboViewer.getControl().setEnabled(showVerify);
		}
		if (remote != null) {
			remoteText.setText(remote);
		}
		appNameCombo.setItems(appListNames);
		if (app != null) {
			appNameCombo.setEnabled(false);
			appNameCombo.setItems(new String[] { app });
			appNameCombo.select(0);
			if (fapplication != null) {
				resetDeployProjectCombo();
			} else {
				this.deployProject = ExpressServerUtils.getExpressDeployProject(server);
				if (deployProject != null) {
					this.deployProjectCombo.setItems(new String[] { deployProject });
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
		for (int i = 0; i < names.length; i++) {
			names[i] = p[i].getName();
		}
		this.deployProjectCombo.setItems(names);
		if (names.length > 0) {
			deployProjectCombo.select(0);
			this.deployProject = names[0];
			browseDestButton.setEnabled(true);
		} else {
			browseDestButton.setEnabled(false);
		}
	}

	private void createWidgets(Composite composite) {
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(composite);
		Label connectionLabel = new Label(composite, SWT.NONE);
		connectionLabel.setText("Connection:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(connectionLabel);
		Combo connectionCombo = new Combo(composite, SWT.DEFAULT);
		this.connectionComboViewer = new ComboViewer(connectionCombo);
		connectionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		connectionComboViewer.setLabelProvider(new ConnectionColumLabelProvider());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(connectionCombo);

		Button newConnectionButton = new Button(composite, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(newConnectionButton);
		newConnectionButton.setText("New...");
		newConnectionButton.addSelectionListener(onNewConnection());

		Label appNameLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);
		appNameCombo = new Combo(composite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(appNameCombo);
		appNameLabel.setText("Application Name: ");

		Label deployLocationLabel = new Label(composite, SWT.NONE);
		deployProjectCombo = new Combo(composite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectCombo);
		deployLocationLabel.setText("Deploy Project: ");

		if (showImportLink) {
			importLink = new Link(composite, SWT.None);
			importLink.setText("<a>Import this application</a>"); //$NON-NLS-1$
			// if we show verify, start import link disabled (wait for verify
			// pressed to enable)
			// Otherwise, not showing verify means we're inside new wizard
			// fragment with no suitable projects
			importLink.setEnabled(!showVerify);
			GridDataFactory.fillDefaults()
					.span(3, 1).applyTo(importLink);
		}

		Label remoteLabel = new Label(composite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);

		Group projectSettings = new Group(composite, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectSettings);
		projectSettings.setLayout(new GridLayout(2, false));

		Label zipDestLabel = new Label(projectSettings, SWT.NONE);
		zipDestLabel.setText("Output Directory: ");

		Composite zipDestComposite = new Composite(projectSettings, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
		zipDestComposite.setLayout(new FormLayout());
		browseDestButton = new Button(zipDestComposite, SWT.PUSH);
		browseDestButton.setText("Browse...");
		browseDestButton.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, null, 0, 100, 0));
		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, 0, 0, browseDestButton, -5));

		// Text
		projectSettings.setText("Project Settings");
		remoteLabel.setText("Remote: ");
	}

	private SelectionListener onNewConnection() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Connection connection = UIUtils.getFirstElement(connectionComboViewer.getSelection(), Connection.class);
				ConnectToOpenShiftWizard wizard = new ConnectToOpenShiftWizard(connection);
				if (WizardUtils.openWizardDialog(
						wizard, connectionComboViewer.getControl().getShell()) == Window.OK) {
					connectionComboViewer.getControl().setEnabled(true);
					connectionComboViewer.setInput(ConnectionsModel.getDefault().getConnections());
					final Connection selectedConnection =
							ConnectionsModel.getDefault().getRecentConnection();
					selectComboConnection(selectedConnection);
				}
			}
		};
	}

	private ISelectionChangedListener onConnectionSelected() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final Connection connection = UIUtils.getFirstElement(event.getSelection(), Connection.class);
				if (connection != null) {
					Job j = new UIUpdatingJob("Verifying connection...") {
						
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							updateModel(connection);
							return Status.OK_STATUS;
						}

						@Override
						protected IStatus updateUI(IProgressMonitor monitor) {
							updateWidgets();
							updateErrorMessage();
							return Status.OK_STATUS;
						}
					};
					callback.executeLongRunning(j);
				}
			}
		};
	}

	private void addListeners() {
		connectionComboViewer.addSelectionChangedListener(onConnectionSelected());

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
					if (ind != -1) {
						fapplication = appList.get(ind);
					}
					resetDeployProjectCombo();
					enableImportLink();
					updateErrorMessage();
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

		if (showImportLink) {
			importLink.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					OpenShiftExpressApplicationWizard wizard =
							new ImportOpenShiftExpressApplicationWizard(connection, null, fapplication);
					WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
					int oldServerCount = ServerCore.getServers().length;
					dialog.create();
					dialog.open();
					if (ServerCore.getServers().length > oldServerCount) {
						// Cancel this wizard, a server has already been created
						// This is reeeaally ugly
						IWizardHandle handle = ((DeploymentTypeUIUtil.NewServerWizardBehaviourCallback) callback)
								.getHandle();
						IWizardContainer container = ((WizardPage) handle).getWizard().getContainer();
						((WizardDialog) container).close();
					}
				}
			});
		}

		deployProjectCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = deployProjectCombo.getSelectionIndex();
				if (index > -1) {
					deployProject = deployProjectCombo.getText();
					deployProjectChanged(deployProject);
				}
			}
		});
	}

	private void deployProjectChanged(String deployProject) {
		if (deployProject != null) {
			IProject depProj = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);
			if (depProj != null && depProj.isAccessible()) {
				String depFolder = ExpressServerUtils.getProjectAttribute(depProj,
						ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, null);
				// ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
				if (depFolder == null)
					deployFolderText.setText(ExpressServerUtils.ATTRIBUTE_DEPLOY_FOLDER_DEFAULT);
				deployFolderText.setEnabled(depFolder == null);
				browseDestButton.setEnabled(depFolder == null);
			}
		}
	}

	private void browsePressed() {
		IFolder f = chooseFolder();
		if (f != null) {
			deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
		}
	}

	private IFolder chooseFolder() {
		if (this.deployProject == null)
			return null;

		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(this.deployProject);

		ILabelProvider lp = new WorkbenchLabelProvider();
		ITreeContentProvider cp = new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), lp,
				cp);
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(p);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		IResource res = p.findMember(new Path(this.deployFolder));
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == Window.OK) {
			return (IFolder) dialog.getFirstResult();
		}
		return null;
	}

	private void updateWidgets() {
		importLink.setEnabled(false);
		if (connection == null
				|| !connection.isConnected()) {
			selectComboConnection(null);
		}
		populateAppNamesCombo();
		enableImportLink();
		resetDeployProjectCombo();
	}

	private void populateAppNamesCombo() {
		if (appNameCombo != null && connection != null) {
			appNameCombo.setItems(appListNames);
			int index = Arrays.asList(appListNames).indexOf(app);
			if (index != -1)
				appNameCombo.select(index);
			else if ((app == null || "".equals(app)) && appListNames.length > 0) {
				int select = 0;
				for (int i = 0; i < appList.size(); i++) {
					IProject[] p = projectsPerApp.get(appList.get(i));
					if (p != null && p.length > 0) {
						select = i;
						break;
					}
				}
				appNameCombo.select(select);
			}
		}
	}

	private void enableImportLink() {
		IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
		importLink.setEnabled(p == null || p.length == 0);
	}

	private void updateErrorMessage() {
		if (!showVerify)
			return;
		callback.setErrorMessage(getErrorString());
	}

	public String getErrorString() {
		String error = null;
		if (appList == null) {
			error = "Please select an existing connection or create a new one.";
		} else if (fdomain == null) {
			error = "Your OpenShift account has not been configured with a domain.";
		} else if (app == null || app.equals("")) {
			error = "Please select an application from the combo below.";
		} else {
			IProject[] p = ExpressServerUtils.findProjectsForApplication(fapplication);
			if (p == null || p.length == 0) {
				error = "Your workspace does not have a project corresponding to " + app + ". Please import one.";
			}
		}
		return error;
	}

	private void updateModel(Connection connection) {
		try {
			// IF we load the applications first, domain gets loaded
			// automatically
			this.appList = connection.getApplications();
			fdomain = connection.getDefaultDomain();
		} catch (NotFoundOpenShiftException nfose) {
			// Credentials work, but no domain, so no applications either
			this.appList = new ArrayList<IApplication>();
			fdomain = null;
		}
		String[] appNames = getAppNamesAsStrings(appList);
		int index = Arrays.asList(appNames).indexOf(app);
		IApplication application = index == -1 ? null : appList.get(index);
		this.appListNames = appNames == null ? new String[0] : appNames;
		this.fapplication = application;
		if (connection.isConnected()) {
			this.connection = connection;
			this.connectionUrl = connection.getUsername();
		} else {
			connection = null;
		}
		updateProjectsPerApp(appList);
		IProject[] possibleProjects = projectsPerApp.get(fapplication);
		this.deployProject =
				possibleProjects == null || possibleProjects.length == 0 ?
						null : possibleProjects[0].getName();

		fillServerWithDetails();
	}

	private void updateProjectsPerApp(List<IApplication> appList) {
		projectsPerApp.clear();
		if (appList != null) {
			for (int i = 0; i < appList.size(); i++) {
				projectsPerApp.put(appList.get(i), ExpressServerUtils.findProjectsForApplication(appList.get(i)));
			}
		}
	}

	private String[] getAppNamesAsStrings(List<IApplication> allApps) {
		if (allApps == null) {
			return new String[] {};
		}
		String[] appNames = new String[allApps.size()];
		for (int i = 0; i < allApps.size(); i++) {
			appNames[i] = allApps.get(i).getName();
		}
		return appNames;
	}

	public void finish(IProgressMonitor monitor) throws CoreException {
		try {
			ConnectionsModel.getDefault().addConnection(connection);
			connection.save();
			fillServerWithDetails();
			updateProjectSettings();
		} catch (OpenShiftException ose) {
			throw new CoreException(new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, ose.getMessage(), ose));
		}
	}

	private void fillServerWithDetails() throws OpenShiftException {
		// Fill the server working copy
		// update the values
		IServerWorkingCopy wc = callback.getServer();
		String host = fapplication == null ? null : fapplication.getApplicationUrl();
		ExpressServerUtils.fillServerWithOpenShiftDetails(wc, host, deployProject, remote);
	}

	private void updateProjectSettings() {
		IProject depProj = ResourcesPlugin.getWorkspace().getRoot().getProject(deployProject);

		String projRemote = ExpressServerUtils.getProjectAttribute(depProj,
				ExpressServerUtils.SETTING_REMOTE_NAME, null);
		String projDepFolder = ExpressServerUtils.getProjectAttribute(depProj,
				ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, null);
		if (projRemote == null && projDepFolder == null) {
			ExpressServerUtils.updateOpenshiftProjectSettings(
					depProj, fapplication, connection, remote, deployFolder);
		}
	}
	
	private void selectComboConnection(final Connection connection) {
		if (connection != null
				&& connection.isConnected()) {
			connectionComboViewer.setSelection(new StructuredSelection(connection));
		} else {
			connectionComboViewer.setSelection(new StructuredSelection());
		}
	}
}
