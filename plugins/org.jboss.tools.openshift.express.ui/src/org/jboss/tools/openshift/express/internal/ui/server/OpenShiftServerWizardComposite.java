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
package org.jboss.tools.openshift.express.internal.ui.server;

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
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
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
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.viewer.ApplicationColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.DomainColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ImportOpenShiftApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.NewOpenShiftApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizard;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;

/**
 * @author Rob Stryker
 * @author Andre Dietisheim
 */
public class OpenShiftServerWizardComposite {
	// How to display errors, set attributes, etc
	protected IServerModeUICallback callback;

	// Widgets
	private Composite composite;
	private Link importLink;
	private ComboViewer connectionComboViewer;
	private ComboViewer domainComboViewer;
	private ComboViewer applicationComboViewer;
	private ComboViewer deployProjectComboViewer;
	protected Text remoteText;
	protected Text deployFolderText;
	protected Button browseDeployFolderButton;

	// Data / Model
	private String remote, deployFolder;
	private IProject deployProject;
	private IDomain domain;
	private IApplication application;
	private ExpressConnection connection;
	private List<IDomain> domains;
	private List<IApplication> applications;
	private IServerWorkingCopy server;
	private Map<IApplication, IProject[]> projectsByApplication = new HashMap<IApplication, IProject[]>();

	public OpenShiftServerWizardComposite(Composite container, IServerModeUICallback callback) {
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
		IApplication application = BehaviorTaskModelUtil.getApplication(callback);
		IDomain domain = BehaviorTaskModelUtil.getDomain(callback);
		updateModel(getConnection(callback), domain, application);
	}
	
	private ExpressConnection getConnection(IServerModeUICallback callback) {
		ExpressConnection connection = BehaviorTaskModelUtil.getConnection(callback);
		if (connection == null) {
			connection = ConnectionsRegistrySingleton.getInstance().getRecentConnection(ExpressConnection.class);
		}
		return connection;
	}
	
	protected String getDeployFolder(IApplication application, IProject deployProject) {
		if (application == null) {
			return null;
		} if (!ProjectUtils.isAccessible(deployProject)) {
			return null;
		}
		return OpenShiftServerUtils.getDefaultDeployFolder(application);
	}

	private void initWidgets() {
		connectionComboViewer.setInput(ConnectionsRegistrySingleton.getInstance().getAll());
		selectConnectionCombo(connection);
		domainComboViewer.setInput(domains);
		selectDomainCombo(domain);
		applicationComboViewer.setInput(applications);
		selectApplicationCombo(application);
		setDeployProjectCombo(application, projectsByApplication);
		remoteText.setText(StringUtils.null2emptyString(remote));
		deployFolderText.setText(StringUtils.null2emptyString(deployFolder));
	}

	private void createWidgets(Composite composite) {
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(composite);

		// connection
		Label connectionLabel = new Label(composite, SWT.NONE);
		connectionLabel.setText(ExpressUIMessages.OpenShiftServerWizardConnection);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(connectionLabel);

		Combo connectionCombo = new Combo(composite, SWT.DEFAULT);
		this.connectionComboViewer = new ComboViewer(connectionCombo);
		connectionComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		connectionComboViewer.setLabelProvider(new ConnectionColumLabelProvider());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(connectionCombo);
		connectionComboViewer.addSelectionChangedListener(onSelectConnection());

		Button newConnectionButton = new Button(composite, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(newConnectionButton);
		newConnectionButton.setText(ExpressUIMessages.OpenShiftServerWizardNew);
		newConnectionButton.addSelectionListener(onNewConnection());

		// domain
		Label domainNameLabel = new Label(composite, SWT.NONE);
		domainNameLabel.setText(ExpressUIMessages.OpenShiftServerWizardDomainName);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(domainNameLabel);

		this.domainComboViewer = new ComboViewer(new Combo(composite, SWT.DEFAULT));
		domainComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		domainComboViewer.setLabelProvider(new DomainColumnLabelProvider());
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(domainComboViewer.getControl());
		domainComboViewer.addSelectionChangedListener(onSelectDomain());

		// application
		Label appNameLabel = new Label(composite, SWT.NONE);
		appNameLabel.setText(ExpressUIMessages.OpenShiftServerWizardApplicationName);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(appNameLabel);

		this.applicationComboViewer = new ComboViewer(new Combo(composite, SWT.DEFAULT));
		applicationComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		applicationComboViewer.setLabelProvider(new ApplicationColumnLabelProvider());
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(applicationComboViewer.getControl());
		applicationComboViewer.addSelectionChangedListener(onSelectApplication());

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
		deployLocationLabel.setText(ExpressUIMessages.OpenShiftServerWizardDeployProject);
		deployProjectComboViewer.addSelectionChangedListener(onSelectDeployProject());

		// import
		importLink = new Link(composite, SWT.None);
		importLink.setText(ExpressUIMessages.OpenShiftServerWizardImportLink); //$NON-NLS-1$
		GridDataFactory.fillDefaults()
				.span(3, 1).applyTo(importLink);
		importLink.addSelectionListener(onClickCreateOrImport());

		// remote
		Label remoteLabel = new Label(composite, SWT.NONE);
		remoteLabel.setText(ExpressUIMessages.OpenShiftServerWizardRemote);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteLabel);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		remoteText.setEditable(false);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(remoteText);
		remoteText.addModifyListener(onModifyRemote());

		Group projectSettings = new Group(composite, SWT.NONE);
		projectSettings.setText(ExpressUIMessages.OpenShiftServerWizardProjectSettings);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectSettings);
		projectSettings.setLayout(new GridLayout(2, false));

		Label zipDestLabel = new Label(projectSettings, SWT.NONE);
		zipDestLabel.setText(ExpressUIMessages.OpenShiftServerWizardOutputDirectory);

		Composite zipDestComposite = new Composite(projectSettings, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(zipDestComposite);
		zipDestComposite.setLayout(new FormLayout());
		browseDeployFolderButton = new Button(zipDestComposite, SWT.PUSH);
		browseDeployFolderButton.setText(ExpressUIMessages.OpenShiftServerWizardBrowse);
		browseDeployFolderButton.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, null, 0, 100, 0));
		browseDeployFolderButton.addSelectionListener(onBrowseDeployFolder());

		deployFolderText = new Text(zipDestComposite, SWT.SINGLE | SWT.BORDER);
		deployFolderText.setLayoutData(UIUtil.createFormData2(0, 5, 100, -5, 0, 0, browseDeployFolderButton, -5));
		deployFolderText.addModifyListener(onModifyDeployFolder());
	}

	private SelectionListener onNewConnection() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ExpressConnection connection = UIUtils.getFirstElement(connectionComboViewer.getSelection(), ExpressConnection.class);
				ConnectionWizard wizard = new ConnectionWizard(connection);
				if (WizardUtils.openWizardDialog(
						wizard, connectionComboViewer.getControl().getShell()) == Window.OK) {
					connectionComboViewer.getControl().setEnabled(true);
					connectionComboViewer.setInput(ConnectionsRegistrySingleton.getInstance().getAll());
					final ExpressConnection selectedConnection =
							ConnectionsRegistrySingleton.getInstance().getRecentConnection(ExpressConnection.class);
					selectConnectionCombo(selectedConnection);
				}
			}
		};
	}

	private ISelectionChangedListener onSelectConnection() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final ExpressConnection selectedConnection = UIUtils.getFirstElement(event.getSelection(), ExpressConnection.class);
				if (selectedConnection == null ||
						(selectedConnection.equals(connection))) {
					return;
				}
				
				callback.executeLongRunning(new UpdateModelJob(selectedConnection, domain, getFirstApplication(applications)));
				updateWidgets();
			}
		};
	}

	protected ModifyListener onModifyDeployFolder() {
		return new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deployFolder = deployFolderText.getText();
			}
		};
	}

	protected ModifyListener onModifyRemote() {
		return new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				remote = remoteText.getText();
			}
		};
	}

	protected SelectionAdapter onClickCreateOrImport() {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				OpenShiftApplicationWizard wizard = null;
				if(application != null){
					wizard = new ImportOpenShiftApplicationWizard(connection, application);
				}else{
					wizard = new NewOpenShiftApplicationWizard(connection);
				}
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
				dialog.create();
				int success = dialog.open();
				if (success == Dialog.OK
						&& wizard.isCreateServerAdapter()) {
					// Cancel this wizard, a server has already been created
					// This is really ugly
					closeWizard(callback);
				} else {
					projectsByApplication = createProjectsByApplication(applications);
					setDeployProjectCombo(application, projectsByApplication);
					updateCreateOrImportLink();
					updateErrorMessage(null);
				}
			}
		};
	}

	private void closeWizard(IServerModeUICallback callback) {
		if (!(callback instanceof DeploymentTypeUIUtil.NewServerWizardBehaviourCallback)) {
			return;
		}
		DeploymentTypeUIUtil.NewServerWizardBehaviourCallback behaviourCallback = (DeploymentTypeUIUtil.NewServerWizardBehaviourCallback) callback;
		IWizardHandle handle = behaviourCallback.getHandle();
		if (!(handle instanceof IWizardPage)) {
			return;
		}
		IWizard wizard = ((IWizardPage) handle).getWizard();
		org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils.close(wizard);
	}

	private ISelectionChangedListener onSelectDeployProject() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				deployProject = UIUtils.getFirstElement(event.getSelection(), IProject.class);
				setRemoteText(application, deployProject);
				setDeploymentFolderText(application, deployProject);
			}
		};
	}

	private ISelectionChangedListener onSelectDomain() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IDomain selectedDomain = UIUtils.getFirstElement(event.getSelection(), IDomain.class);				
				if (selectedDomain == null
						|| selectedDomain.equals(domain)) {
					return;
				}	
				callback.executeLongRunning(new UpdateModelJob(connection, selectedDomain, application));
				updateWidgets();
			}
		};
	}

	private ISelectionChangedListener onSelectApplication() {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				application = UIUtils.getFirstElement(event.getSelection(), IApplication.class);
				setDeployProjectCombo(application, projectsByApplication);
				updateCreateOrImportLink();
				setDeploymentFolderText(application, deployProject);
				setRemoteText(application, deployProject);
				updateErrorMessage(null);
			}
		};
	}

	private String getRemote(IApplication application, IProject project) {
		if (application == null
				|| project == null
				|| !ProjectUtils.isAccessible(project)) {
			return null;
		}
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
			ExpressUIActivator.log(
					NLS.bind(ExpressUIMessages.OpenShiftServerWizardCouldNotGetRemotePointing,
							application.getGitUrl(), project.getName()), e);
			return null;
		}
	}

	private SelectionAdapter onBrowseDeployFolder() {
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
		dialog.setTitle(ExpressUIMessages.OpenShiftServerWizardDeployLocation);
		dialog.setMessage(ExpressUIMessages.OpenShiftServerWizardPleaseChooseLocation);
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
		setDomainComboInput(domains);
		selectDomainCombo(domain);
		setApplicationComboInput(applications);
		selectApplicationCombo(application);
		selectDeployProjectCombo(getImportedProjects(application, projectsByApplication));
		updateCreateOrImportLink();
		updateErrorMessage(null);
	}

	protected void setRemoteText(IApplication application, IProject deployProject) {
		if (application == null
				|| !ProjectUtils.isAccessible(deployProject)) {
			remoteText.setEnabled(false);
			return;
		}

		String remoteName = getRemote(application, deployProject);
		remoteText.setText(remoteName);
		remoteText.setEnabled(!StringUtils.isEmpty(remoteName));
	}
	
	protected void setDeploymentFolderText(IApplication application, IProject deployProject) {
		if (application == null
				|| !ProjectUtils.isAccessible(deployProject)) {
			deployFolderText.setEnabled(false);
			browseDeployFolderButton.setEnabled(false);
			return;
		}

		deployFolder = getDeployFolder(application, deployProject);
		deployFolderText.setText(StringUtils.null2emptyString(deployFolder));
		deployFolderText.setEnabled(!StringUtils.isEmpty(deployFolder));
		browseDeployFolderButton.setEnabled(true);		
	}

	private void setDomainComboInput(List<IDomain> domains) {
		if (domains == null) {
			domainComboViewer.setInput(Collections.emptyList());
		} else {
			domainComboViewer.setInput(domains);
		}
	}

	private void setApplicationComboInput(List<IApplication> applications) {
		if (applications == null) {
			applicationComboViewer.setInput(Collections.emptyList());
		} else {
			applicationComboViewer.setInput(applications);
		}
	}

	private void updateCreateOrImportLink() {
		boolean enabled = true;
		if(application != null){
			IProject[] p = OpenShiftServerUtils.findProjectsForApplication(application);
			enabled = p == null || p.length == 0;
			importLink.setText(ExpressUIMessages.OpenShiftServerWizardImportLink);
		}else{
			importLink.setText(ExpressUIMessages.OpenShiftServerWizardCreateLink);
		}
		importLink.setEnabled(enabled);
	}

	private void updateErrorMessage(String message) {
		callback.setErrorMessage(createErrorMessage(message));
	}

	public String createErrorMessage() {
		return createErrorMessage(null);
	}
	
	public String createErrorMessage(String message) {
		String error = null;
		if (message != null) {
			error = message;
		} else if (connection == null) {
			error = ExpressUIMessages.OpenShiftServerWizardPleaseSelectConnection;
		} else if (domains == null) {
			error = NLS.bind(ExpressUIMessages.OpenShiftServerWizardPleaseCreateDomain, connection.getId());
		} else if (applications.isEmpty()) {
			error = ExpressUIMessages.OpenShiftServerWizardPleaseCreateApplication;
		} else if (application == null) {
			error = ExpressUIMessages.OpenShiftServerWizardPleaseSelectApplication;
		} else {
			IProject[] p = OpenShiftServerUtils.findProjectsForApplication(application);
			if (p == null || p.length == 0) {
				error = NLS.bind(
					ExpressUIMessages.OpenShiftServerWizardYourWorkspaceDoesNotHaveProject,
					application.getName());
			}
		}
		return error;
	}

	private void updateModel(ExpressConnection connection, IDomain domain, IApplication application) {
		this.connection = connection;
		this.domains = safeGetDomains(connection);
		this.domain = getDomain(domain, domains);
		this.applications = safeGetApplications(this.domain);
		this.projectsByApplication = createProjectsByApplication(applications);
		this.application = getApplication(application, applications);
		this.deployProject = getDeployProject(this.application);
		this.deployFolder = getDeployFolder(this.application, deployProject);
		this.remote = getRemote(this.application, deployProject);
		configureServer(this.application, this.domain, this.remote, this.deployProject, this.deployFolder, callback.getServer());
	}

	protected IDomain getDomain(final IDomain domain, final List<IDomain> domains) {
		if (domain == null) {
			return getFirstDomain(domains);
		} else if (domains != null
				&& domains.indexOf(domain) == -1) {
			// domain switched, current domain not contained within new list
			return getFirstDomain(domains);
		} else {
			return domain;
		}
	}

	private IDomain getFirstDomain(final List<IDomain> domains) {
		if (domains != null
				&& domains.size() > 0) {
			return domains.get(0);
		}
		return null;
	}

	protected IApplication getApplication(final IApplication application, final List<IApplication> applications) {
		if (application == null) {
			return getFirstApplication(applications);
		} else if (applications != null
				&& applications.indexOf(application) == -1){ 
			// domain changed, application not within list of applications of new domain
			return getFirstApplication(applications);
		} else {
			return application;
		}
	}

	private IProject getDeployProject(IApplication application) {
		if (application == null) {
			return null;
		}
		IProject[] projects = OpenShiftServerUtils.findProjectsForApplication(application);
		if (projects == null
				|| projects.length < 1) {
			return null;
		}
		
		return projects[0];
	}

	private List<IApplication> safeGetApplications(IDomain domain) {
		try {
			if (domain == null) {
				return null;
			}
			return domain.getApplications();
		} catch (NotFoundOpenShiftException nfose) {
			// Credentials work, but no domain, so no applications either
			return null;
		}
	}

	private List<IDomain> safeGetDomains(ExpressConnection connection) {
		try {
			if (connection == null) {
				return null;
			}
			return connection.getDomains();
		} catch (NotFoundOpenShiftException e) {
			// Credentials work, but no domain, so no applications either
			return Collections.emptyList();
		} catch (OpenShiftException e) {
			// Credentials work, but no domain, so no applications either
			updateErrorMessage(NLS.bind(ExpressUIMessages.OpenShiftServerWizardCouldNotLoadDomains, connection.getId(), e.getMessage()));
			return null;
		}
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
						OpenShiftServerUtils.findProjectsForApplication(applications.get(i)));
			}
		}
		return projectsByApplication;
	}

	public void performFinish(IProgressMonitor monitor) throws CoreException {
		configureServer(application, domain, remote, deployProject, deployFolder, callback.getServer());
		updateProjectSettings(application, domain, remote, deployProject, deployFolder, connection);
	}

	private void configureServer(IApplication application, IDomain domain, String remote, IProject deployProject, String deployFolder,
			IServerWorkingCopy server) throws OpenShiftException {
		String serverName = OpenShiftServerUtils.getDefaultServerName(application);
		OpenShiftServerUtils.fillServerWithOpenShiftDetails(
				server, serverName, deployProject, deployFolder, remote, application, domain);
	}

	private void updateProjectSettings(IApplication application, IDomain domain, String remote, IProject deployProject, String deployFolder, ExpressConnection connection) {
		String projRemote = 
				OpenShiftServerUtils.getProjectAttribute(deployProject, OpenShiftServerUtils.SETTING_REMOTE_NAME, null);
		String projDepFolder = 
				OpenShiftServerUtils.getProjectAttribute(deployProject, OpenShiftServerUtils.SETTING_DEPLOY_FOLDER_NAME, null);
		if (projRemote == null 
				&& projDepFolder == null) {
			OpenShiftServerUtils.updateOpenshiftProjectSettings(
					deployProject, application, domain, connection, remote, deployFolder);
		}
	}

	private void selectConnectionCombo(final ExpressConnection connection) {
		IStructuredSelection selection = new StructuredSelection();
		if (connection != null) {
			selection = new StructuredSelection(connection);
		}
		connectionComboViewer.setSelection(selection);
	}

	private void selectDomainCombo(final IDomain domain) {
		IStructuredSelection selection = new StructuredSelection();
		if (domain != null) {
			selection = new StructuredSelection(domain);
		}
		domainComboViewer.setSelection(selection);
	}

	protected void selectApplicationCombo(IApplication application) {
		ISelection selection = new StructuredSelection();
		if (application != null) {
			selection = new StructuredSelection(application);
		}
		applicationComboViewer.setSelection(selection);
	}

	private void setDeployProjectCombo(IProject[] importedProjects) {
		deployProjectComboViewer.setInput(importedProjects);
		selectDeployProjectCombo(importedProjects);
	}

	private void setDeployProjectCombo(IApplication application, Map<IApplication, IProject[]> projectsByApplication) {
		setDeployProjectCombo(getImportedProjects(application, projectsByApplication));
	}
	
	private IProject[] getImportedProjects(IApplication application, Map<IApplication, IProject[]> projectsByApplication) {
		IProject[] importedProjects = new IProject[0];
		if (application != null) {
			importedProjects = projectsByApplication.get(application);	
		}
		return importedProjects;
	}

	private void selectDeployProjectCombo(final IProject[] importedProjects) {
		IStructuredSelection selection = new StructuredSelection();
		if (importedProjects != null
				&& importedProjects.length > 0) {
			selection = new StructuredSelection(importedProjects[0]);
		}
		deployProjectComboViewer.setSelection(selection);
	}	

	private class UpdateModelJob extends Job {
		
		private ExpressConnection connection;
		private IDomain domain;
		private IApplication application;

		private UpdateModelJob(ExpressConnection connection, IDomain domain, IApplication application) {
			super(NLS.bind(ExpressUIMessages.OpenShiftServerWizardFetchingDomainsAndApplications, connection.getUsername()));
			this.connection = connection;
			this.domain = domain;
			this.application = application;
			
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			updateModel(this.connection, this.domain, this.application);
			return Status.OK_STATUS;
		}
	}
}
