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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.connection.ConnectionsModelSingleton;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.ApplicationColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ImportOpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftExpressApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.connection.ConnectToOpenShiftWizard;

import com.openshift.client.IApplication;
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
	private Composite composite;
	private Link importLink;
	private ComboViewer connectionComboViewer;
	private ComboViewer applicationComboViewer;
	private ComboViewer deployProjectComboViewer;
	protected Text remoteText;
	protected Text deployFolderText;
	protected Button browseDestButton;

	// Data / Model
	private String remote, deployFolder;
	private IProject deployProject;
	private IApplication application;
	private Connection connection;
	private List<IApplication> applications;
	private IServerWorkingCopy server;
	private Map<IApplication, IProject[]> projectsByApplication = new HashMap<IApplication, IProject[]>();

	public ExpressDetailsComposite(Composite container, IServerModeUICallback callback) {
		this.callback = callback;
		this.server = callback.getServer();
		this.composite = container;
		initModel(callback, server);
		createWidgets(container);
		initWidgets();
	}

	public Composite getComposite() {
		return composite;
	}

	private void initModel(IServerModeUICallback callback, IServerAttributes server) {
		this.remote = ExpressServerUtils.getExpressRemoteName(server);
		updateModel(getConnection(callback), ExpressServerUtils.getApplication(callback));
	}
	
	private Connection getConnection(IServerModeUICallback callback) {
		Connection connection = ExpressServerUtils.getConnection(callback);
		if (connection == null) {
			connection = ConnectionsModelSingleton.getInstance().getRecentConnection();
		}
		return connection;
	}
	
	protected String getDeployFolder(IServerAttributes server, IApplication application) {
		if (application == null) {
			return null;
		} 
		return ExpressServerUtils.getExpressDeployFolder(server, application);
	}

	private void initWidgets() {
		connectionComboViewer.setInput(ConnectionsModelSingleton.getInstance().getConnections());
		selectConnectionCombo(connection);
		applicationComboViewer.setInput(applications);
		selectApplicationCombo(application);
		setDeployProjectComboInput(application);
		selectDeployProjectCombo(application);
		remoteText.setText(StringUtils.null2emptyString(remote));
		deployFolderText.setText(StringUtils.null2emptyString(deployFolder));
	}

	private void createWidgets(Composite composite) {
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(composite);

		// connection
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
		connectionComboViewer.addSelectionChangedListener(onConnectionSelected());

		Button newConnectionButton = new Button(composite, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(newConnectionButton);
		newConnectionButton.setText("New...");
		newConnectionButton.addSelectionListener(onNewConnection());

		// application
		Label appNameLabel = new Label(composite, SWT.NONE);
		appNameLabel.setText("Application Name: ");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);

		this.applicationComboViewer = new ComboViewer(new Combo(composite, SWT.DEFAULT));
		applicationComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		applicationComboViewer.setLabelProvider(new ApplicationColumnLabelProvider());
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(applicationComboViewer.getControl());
		applicationComboViewer.addSelectionChangedListener(onApplicationSelected());

		// deploy project
		Label deployLocationLabel = new Label(composite, SWT.NONE);
		this.deployProjectComboViewer = new ComboViewer(new Combo(composite, SWT.DEFAULT));
		deployProjectComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		deployProjectComboViewer.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (!(element instanceof IProject)) {
					return super.getText(element);
				}
				return ((IProject) element).getName();
			}
		});
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(deployProjectComboViewer.getControl());
		deployLocationLabel.setText("Deploy Project: ");
		deployProjectComboViewer.addSelectionChangedListener(onDeployProjectSelected());

		// import
		importLink = new Link(composite, SWT.None);
		importLink.setText("<a>Import this application</a>"); //$NON-NLS-1$
		GridDataFactory.fillDefaults()
				.span(3, 1).applyTo(importLink);
		importLink.addSelectionListener(onImportClicked());

		// remote
		Label remoteLabel = new Label(composite, SWT.NONE);
		remoteLabel.setText("Remote: ");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		remoteText.setEditable(false);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
		remoteText.addModifyListener(onRemoteModified());

		Group projectSettings = new Group(composite, SWT.NONE);
		projectSettings.setText("Project Settings");
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
		browseDestButton.addSelectionListener(onBrowseDeployFolder(deployFolderText));

		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, 0, 0, browseDestButton, -5));
		deployFolderText.addModifyListener(onDeployFolderModified());
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
					connectionComboViewer.setInput(ConnectionsModelSingleton.getInstance().getConnections());
					final Connection selectedConnection =
							ConnectionsModelSingleton.getInstance().getRecentConnection();
					selectConnectionCombo(selectedConnection);
				}
			}
		};
	}

	private ISelectionChangedListener onConnectionSelected() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final Connection selectedConnection = UIUtils.getFirstElement(event.getSelection(), Connection.class);
				if (selectedConnection == null ||
						(connection != null
						&& connection.equals(selectedConnection))) {
					return;
				}

				Job j = new Job("Verifying connection...") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						updateModel(selectedConnection, getFirstApplication(applications));
						return Status.OK_STATUS;
					}
				};
				callback.executeLongRunning(j);
				updateWidgets();
				updateErrorMessage();
			}
		};
	}

	protected ModifyListener onDeployFolderModified() {
		return new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deployFolder = deployFolderText.getText();
			}
		};
	}

	protected ModifyListener onRemoteModified() {
		return new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				remote = remoteText.getText();
			}
		};
	}

	protected SelectionAdapter onImportClicked() {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				OpenShiftExpressApplicationWizard wizard =
						new ImportOpenShiftExpressApplicationWizard(connection, null, application);
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
				int oldServerCount = ServerCore.getServers().length;
				dialog.create();
				dialog.open();
				if (ServerCore.getServers().length > oldServerCount) {
					// Cancel this wizard, a server has already been created
					// This is really ugly
					IWizardHandle handle =
							((DeploymentTypeUIUtil.NewServerWizardBehaviourCallback) callback).getHandle();
					org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils.close(((WizardPage) handle).getWizard());
				}
			}
		};
	}

	private ISelectionChangedListener onDeployProjectSelected() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				deployProject = UIUtils.getFirstElement(event.getSelection(), IProject.class);

				if (deployProject != null
						&& deployProject.isAccessible()) {

					String remoteName = getRemoteConfig(remote, application, deployProject);
					remoteText.setText(remoteName);
					remoteText.setEnabled(!StringUtils.isEmpty(remoteName));

					String deployFolder = ExpressServerUtils.getExpressDeployFolder(server, application);
					deployFolderText.setText(StringUtils.null2emptyString(deployFolder));
					deployFolderText.setEnabled(!StringUtils.isEmpty(deployFolder));
					browseDestButton.setEnabled(!StringUtils.isEmpty(deployFolder));
				} else {
					deployFolderText.setEnabled(false);
					browseDestButton.setEnabled(false);
					remoteText.setEnabled(false);
				}
			}
		};
	}

	private ISelectionChangedListener onApplicationSelected() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				application = UIUtils.getFirstElement(event.getSelection(), IApplication.class);
				setDeployProjectComboInput(application);
				selectDeployProjectCombo(application);
				enableImportLink(application);
				updateErrorMessage();
			}
		};
	}

	private String getRemoteConfig(String remote, IApplication application, IProject project) {
		try {
			Repository repository = EGitUtils.getRepository(project);
			if (repository == null) {
				return null;
			}
			Pattern gitURIPattern = Pattern.compile(RegExUtils.escapeRegex(application.getGitUrl()));
			RemoteConfig remoteConfig = EGitUtils.getRemoteByUrl(gitURIPattern, repository);
			if (remoteConfig == null) {
				return null;
			}
			return remoteConfig.getName();
		} catch (CoreException e) {
			OpenShiftUIActivator.log(
					NLS.bind("Could not get remote pointing to {0} for project {1}",
							application.getGitUrl(), project.getName()), e);
			return null;
		}
	}

	private SelectionAdapter onBrowseDeployFolder(final Text deployFolderText) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IFolder f = chooseFolder();
				if (f != null) {
					deployFolderText.setText(f.getFullPath().removeFirstSegments(1).makeRelative().toOSString());
				}
			}
		};
	}

	private IFolder chooseFolder() {
		if (this.deployProject == null) {
			return null;
		}

		ILabelProvider lp = new WorkbenchLabelProvider();
		ITreeContentProvider cp = new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog =
				new ElementTreeSelectionDialog(Display.getDefault().getActiveShell(), lp, cp);
		dialog.setTitle("Deploy Location");
		dialog.setMessage("Please choose a location to put zipped projects");
		dialog.setInput(deployProject);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		IResource res = deployProject.findMember(new Path(this.deployFolder));
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		if (dialog.open() == Window.OK) {
			return (IFolder) dialog.getFirstResult();
		}
		return null;
	}

	private void updateWidgets() {
		if (connection == null) {
			selectConnectionCombo(null);
		}
		setApplicationComboInput(applications);
		selectApplicationCombo(application);
		selectDeployProjectCombo(application);
		enableImportLink(application);
		updateErrorMessage();
	}

	private void setApplicationComboInput(List<IApplication> applications) {
		if (applications == null) {
			applicationComboViewer.setInput(Collections.emptyList());
		} else {
			applicationComboViewer.setInput(applications);
		}
	}

	private void enableImportLink(IApplication application) {
		IProject[] p = ExpressServerUtils.findProjectsForApplication(application);
		importLink.setEnabled(p == null || p.length == 0);
	}

	private void updateErrorMessage() {
		callback.setErrorMessage(getErrorString());
	}

	public String getErrorString() {
		String error = null;
		if (applications == null) {
			error = "Please select an existing connection or create a new one.";
		} else if (application == null) {
			error = "Please select an application from the combo below.";
		} else {
			IProject[] p = ExpressServerUtils.findProjectsForApplication(application);
			if (p == null || p.length == 0) {
				error = "Your workspace does not have a project corresponding to " + application.getName()
						+ ". Please import one.";
			}
		}
		return error;
	}

	private void updateModel(Connection connection, IApplication application) {
		this.connection = connection;
		this.applications = safeGetApplications(connection);
		this.projectsByApplication = createProjectsByApplication(applications);
		if (application == null) {
			this.application = getFirstApplication(applications);	
		} else {
			this.application = application;
		}
		this.deployProject = getDeployProject(application);
		
		fillServerWithDetails(application, remote, deployProject, callback);
	}

	private IProject getDeployProject(IApplication application) {
		if (application == null) {
			return null;
		}
		IProject[] projects = ExpressServerUtils.findProjectsForApplication(application);
		if (projects == null
				|| projects.length < 1) {
			return null;
		}
		
		return projects[0];
	}

	private List<IApplication> safeGetApplications(Connection connection) {
		List<IApplication> applications = new ArrayList<IApplication>();
		try {
			if (connection != null) {
				applications = connection.getApplications();
			}
		} catch (NotFoundOpenShiftException nfose) {
			// Credentials work, but no domain, so no applications either
		}
		return applications;
	}

	private IApplication getFirstApplication(List<IApplication> applications) {
		IApplication application = null; 
		if (applications != null
				&& applications.size() > 0) {
			application = applications.get(0);
		}
		return application;
	}
	
	private Map<IApplication, IProject[]> createProjectsByApplication(List<IApplication> applications) {
		Map<IApplication, IProject[]> projectsByApplication = new HashMap<IApplication, IProject[]>();
		if (applications != null) {
			for (int i = 0; i < applications.size(); i++) {
				projectsByApplication.put(applications.get(i),
						ExpressServerUtils.findProjectsForApplication(applications.get(i)));
			}
		}
		return projectsByApplication;
	}

	public void performFinish(IProgressMonitor monitor) throws CoreException {
		fillServerWithDetails(application, remote, deployProject, callback);
		updateProjectSettings();
	}

	private void fillServerWithDetails(IApplication application, String remote, IProject deployProject, IServerModeUICallback callback) throws OpenShiftException {
		String host = null;
		String applicationName = null;
		if (application != null) {
			host = application.getApplicationUrl();
			applicationName = application.getName();
		}

		String deployProjectName = null;
		if (deployProject != null) {
			deployProjectName = deployProject.getName();
		}

		ExpressServerUtils.fillServerWithOpenShiftDetails(callback.getServer(), host, deployProjectName, remote, applicationName);
	}

	private void updateProjectSettings() {
		String projRemote = ExpressServerUtils.getProjectAttribute(
				deployProject, ExpressServerUtils.SETTING_REMOTE_NAME, null);
		String projDepFolder = ExpressServerUtils.getProjectAttribute(
				deployProject, ExpressServerUtils.SETTING_DEPLOY_FOLDER_NAME, null);
		if (projRemote == null 
				&& projDepFolder == null) {
			ExpressServerUtils.updateOpenshiftProjectSettings(
					deployProject, application, connection, remote, deployFolder);
		}
	}

	private void selectConnectionCombo(final Connection connection) {
		IStructuredSelection selection = new StructuredSelection();
		if (connection != null
				&& connection.isConnected()) {
			selection = new StructuredSelection(connection);
		}
		connectionComboViewer.setSelection(selection);
	}

	protected void selectApplicationCombo(IApplication application) {
		ISelection selection = new StructuredSelection();
		if (application != null) {
			selection = new StructuredSelection(application);
		}
		applicationComboViewer.setSelection(selection);
	}

	private void setDeployProjectComboInput(IApplication application) {
		List<IProject> deployProjects = new ArrayList<IProject>();
		if (application != null) {
			IProject[] projects = projectsByApplication.get(application);
			deployProjects.addAll(Arrays.asList(projects));
		}
		deployProjectComboViewer.setInput(deployProjects);
	}

	private void selectDeployProjectCombo(final IApplication application) {
		IStructuredSelection selection = new StructuredSelection();
		if (application != null) {
			IProject[] projects = projectsByApplication.get(application);
			if (projects != null
					&& projects.length > 0)
				selection = new StructuredSelection(projects[0]);
		}
		deployProjectComboViewer.setSelection(selection);
	}

}
