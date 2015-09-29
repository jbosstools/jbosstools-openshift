/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.server;

import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil;
import org.jboss.ide.eclipse.as.ui.editor.IDeploymentTypeUI.IServerModeUICallback;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsView {
	// How to display errors, set attributes, etc
	protected IServerModeUICallback callback;

	// Widgets
	private DataBindingContext dbc;
	private boolean showConnection;
	private ComboViewer connectionViewer;
	private ServerSettingsViewModel model;

	public ServerSettingsView(IServerModeUICallback callback) {
		this(true, callback);
	}
	
	public ServerSettingsView(boolean showConnection, IServerModeUICallback callback) {
		this.showConnection = showConnection;
		this.callback = callback;
		this.model = new ServerSettingsViewModel(callback.getServer(), OpenShiftServerTaskModelAccessor.getConnection(callback));
	}

	private IProject getSelectedProject() {
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = win.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof org.eclipse.core.resources.IResource) {
				return (((org.eclipse.core.resources.IResource)element).getProject());
			}
		}
		return null;
	}

	public Control createControls(Composite parent) {
		this.dbc = new DataBindingContext();

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.margins(10, 10)
			.applyTo(container);
		
		// connection
		Composite connectionComposite = createConnectionWidgets(container, showConnection);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).exclude(!showConnection)
			.applyTo(connectionComposite);

		// project
		Composite projectComposite = createProjectWidgets(container);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(projectComposite);
		
		// pod path
		Composite deploymentComposite = createDeploymentWidgets(container);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(deploymentComposite);

		// services
		Composite serviceComposite = createServiceWidgets(container);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(serviceComposite);

		final IProject selectedProject = getSelectedProject();

		callback.executeLongRunning(new Job("Loading projects and services...") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				model.loadResources();
				//initializing the project combo with the project selected in the workspace
				if (selectedProject != null) {
					model.setDeployProject(selectedProject);
				}
				return Status.OK_STATUS;
			}
		});
		
		return container;
	}

	/**
	 * Open a dialog box to select an open project when clicking on the 'Browse' button.
	 * 
	 * @return
	 */
	private SelectionListener onBrowseProjects(ServerSettingsViewModel model, final Shell shell) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectExistingProjectDialog dialog = 
						new SelectExistingProjectDialog("Select a project to deploy", shell);
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					if (selectedProject instanceof IProject) {
						model.setDeployProject((org.eclipse.core.resources.IProject) selectedProject);
					}
				}
			}
		};
	}

	private Composite createConnectionWidgets(Composite parent, boolean showConnection) {
		Group connectionGroup = new Group(parent, SWT.NONE);
		connectionGroup.setText("connection");
		connectionGroup.setVisible(showConnection);
		GridLayoutFactory.fillDefaults()
			.margins(10, 10)
			.applyTo(connectionGroup);

		// additional nesting required because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
		Composite container = new Composite(connectionGroup, SWT.NONE);
		container.setVisible(showConnection);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).exclude(!showConnection)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(3)
			.applyTo(container);

		Label connectionLabel = new Label(container, SWT.NONE);
		connectionLabel.setText("Connection:");
		connectionLabel.setVisible(showConnection);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).exclude(!showConnection)
				.applyTo(connectionLabel);

		Combo connectionCombo = new Combo(container, SWT.DEFAULT);
		this.connectionViewer = new ComboViewer(connectionCombo);
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ConnectionColumLabelProvider());
		connectionCombo.setVisible(showConnection);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).exclude(!showConnection)
				.applyTo(connectionCombo);
		connectionViewer.setInput(
				BeanProperties.list(ServerSettingsViewModel.PROPERTY_CONNECTIONS).observe(model));
		ValueBindingBuilder	
			.bind(ViewerProperties.singleSelection().observe(connectionViewer))
			.to(BeanProperties.value(ServerSettingsViewModel.PROPERTY_CONNECTION).observe(model));

		Button newConnectionButton = new Button(container, SWT.PUSH);
		newConnectionButton.setText("New...");
		newConnectionButton.setVisible(showConnection);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).exclude(!showConnection)
			.applyTo(newConnectionButton);
		newConnectionButton.addSelectionListener(onNewConnection());
		
		return connectionGroup;
	}

	private Composite createProjectWidgets(Composite parent) {
		Composite projectComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10)
			.applyTo(projectComposite);

		Label projectLabel = new Label(projectComposite, SWT.NONE);
		projectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(projectLabel);

		StructuredViewer projectsViewer = new ComboViewer(projectComposite);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(projectsViewer.getControl());

		projectsViewer.setContentProvider(new ObservableListContentProvider());
		projectsViewer.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(Object element) {
				if (!(element instanceof IProject)) {
					return null;
				}

				return ((IProject) element).getName();
			}
		});
		projectsViewer.setInput(
				BeanProperties.list(ServerSettingsViewModel.PROPERTY_PROJECTS).observe(model));

		IObservableValue selectedProjectObservable = ViewerProperties.singleSelection().observe(projectsViewer);
		Binding selectedProjectBinding = 
				ValueBindingBuilder.bind(selectedProjectObservable)
					.validatingAfterConvert(new IValidator() {

						@Override
						public IStatus validate(Object value) {
							if (value instanceof IProject) {
								return ValidationStatus.ok();
							}
							return ValidationStatus.cancel("Please choose a project to deploy.");
						}
					})
					.to(BeanProperties.value(ServerSettingsViewModel.PROPERTY_DEPLOYPROJECT)
					.observe(model))
					.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		// browse projects
		Button browseProjectsButton = new Button(projectComposite, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.hint(100, SWT.DEFAULT)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects(model, browseProjectsButton.getShell()));

		return projectComposite;
	}

	private Composite createDeploymentWidgets(Composite parent) {
		Composite deploymentComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10)
			.applyTo(deploymentComposite);

		Label deployPathLabel = new Label(deploymentComposite, SWT.NONE);
		deployPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(deployPathLabel);

		Text deployPathText = new Text(deploymentComposite, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(deployPathText);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(deployPathText))
			.validatingAfterConvert(new IValidator() {

				@Override
				public IStatus validate(Object value) {
					String path = (String) value;
					if (StringUtils.isEmpty(value)) {
						return ValidationStatus.cancel("Please provide a path to deploy to on the pod.");
					}
					if (!Path.isValidPosixPath(path)) {
						return ValidationStatus.error("Please provide a valid path to deploy to on the pod");
					}
					return ValidationStatus.ok();
				}
				
			})
			.to(BeanProperties.value(ServerSettingsViewModel.PROPERTY_POD_PATH).observe(model))
			.in(dbc);
		
		return deploymentComposite;
	}

	private Composite createServiceWidgets(Composite container) {
		Group servicesGroup = new Group(container, SWT.NONE);
		servicesGroup.setText("Services");
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10,10)
			.applyTo(servicesGroup);

		Label selectorLabel = new Label(servicesGroup, SWT.NONE);
		selectorLabel.setText("Selector:");
		Text selectorText = UIUtils.createSearchText(servicesGroup);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(selectorText);

		final TreeViewer servicesViewer = createServicesTreeViewer(servicesGroup, selectorText);
		BeanProperties.list(ServerSettingsViewModel.PROPERTY_SERVICE_ITEMS).observe(model)
			.addListChangeListener(onServiceItemsChanged(servicesViewer));
		GridDataFactory.fillDefaults()
			.span(2, 1).align(SWT.FILL, SWT.FILL).hint(SWT.DEFAULT, 160).grab(true, true)
			.applyTo(servicesViewer.getControl());
		selectorText.addModifyListener(onFilterTextModified(servicesViewer));
		IViewerObservableValue selectedServiceTreeItem = ViewerProperties.singleSelection().observe(servicesViewer);
		ValueBindingBuilder
				.bind(selectedServiceTreeItem)
				.converting(new ObservableTreeItem2ModelConverter(IService.class))
				.validatingAfterConvert(new IValidator() {
					
					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof IService)) {
							return ValidationStatus.cancel("Please select a service that this adapter will be bound to.");
						} else {
							return ValidationStatus.ok();
						}
						
					}
				})
				.to(BeanProperties.value(ServerSettingsViewModel.PROPERTY_SERVICE).observe(model))
				.converting(new Model2ObservableTreeItemConverter(new ServerSettingsViewModel.ServiceTreeItemsFactory()))
				.in(dbc);

		// details
		Label detailsLabel = new Label(servicesGroup, SWT.NONE);
		detailsLabel.setText("Service Details:");
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL)
				.applyTo(detailsLabel);
		
		Composite detailsContainer = new Composite(servicesGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
				.applyTo(detailsContainer);
		IObservableValue selectedService = new WritableValue();
		ValueBindingBuilder
			.bind(selectedServiceTreeItem)
			.converting(new ObservableTreeItem2ModelConverter())
			.to(selectedService)
			.notUpdatingParticipant()
			.in(dbc);
		new ServiceDetailViews(selectedService, detailsContainer, dbc).createControls();

		return servicesGroup;
	}

	private IListChangeListener onServiceItemsChanged(final TreeViewer servicesViewer) {
		return new IListChangeListener() {
			
			@Override
			public void handleListChange(ListChangeEvent event) {
				servicesViewer.expandAll();
			}
		};
	}

	private SelectionListener onNewConnection() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Connection connection = UIUtils.getFirstElement(connectionViewer.getSelection(), Connection.class);
				ConnectionWizard wizard = new ConnectionWizard(connection);
				if (WizardUtils.openWizardDialog(
						wizard, connectionViewer.getControl().getShell()) == Window.OK) {
					connectionViewer.getControl().setEnabled(true);
					connectionViewer.setInput(ConnectionsRegistrySingleton.getInstance().getAll());
					final Connection selectedConnection =
							ConnectionsRegistrySingleton.getInstance().getRecentConnection(Connection.class);
					model.setConnection(selectedConnection);
				}
			}
		};
	}

	private TreeViewer createServicesTreeViewer(Composite parent, Text selectorText) {
		TreeViewer applicationTemplatesViewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] {
						BeanProperties.list(ServerSettingsViewModel.PROPERTY_SERVICE_ITEMS),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		applicationTemplatesViewer.setContentProvider(contentProvider);
		applicationTemplatesViewer.setLabelProvider(new ServicesViewLabelProvider());
		applicationTemplatesViewer.addFilter(new ServiceViewerFilter(selectorText));
		applicationTemplatesViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		applicationTemplatesViewer.setInput(model);
		return applicationTemplatesViewer;
	}	

	private class ServicesViewLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (!(element instanceof ObservableTreeItem)) {
				return;
			} 
			if (!(((ObservableTreeItem) element).getModel() instanceof IResource)) {
					return;
			}
			
			IResource resource = (IResource) ((ObservableTreeItem) element).getModel();
			
			StyledString text = new StyledString();
			if (resource instanceof com.openshift.restclient.model.IProject) {
				createProjectLabel(text, (com.openshift.restclient.model.IProject) resource);
			} else if (resource instanceof IService) {
				createServiceLabel(text, (IService) resource);
			}

			cell.setText(text.toString());
			cell.setStyleRanges(text.getStyleRanges());
			super.update(cell);
		}
	
		private void createProjectLabel(StyledString text, com.openshift.restclient.model.IProject resource) {
			text.append(resource.getName());
		}

		private void createServiceLabel(StyledString text, IService service) {
			text.append(service.getName());
			String selectorsDecoration = org.jboss.tools.openshift.common.core.utils.StringUtils.toString(service.getSelector());
			if (!StringUtils.isEmpty(selectorsDecoration)) {
				text.append(" ", StyledString.DECORATIONS_STYLER);
				text.append(selectorsDecoration, StyledString.DECORATIONS_STYLER);
			}
		}
	}

	protected ModifyListener onFilterTextModified(final TreeViewer applicationTemplatesViewer) {
		return new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				applicationTemplatesViewer.refresh();
				applicationTemplatesViewer.expandAll();
			}
		};
	}
	
	class ServiceViewerFilter extends ViewerFilter {

		private Text filterText;

		public ServiceViewerFilter(Text filterText) {
			Assert.isLegal(!DisposeUtils.isDisposed(filterText));
			this.filterText = filterText;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!(element instanceof ObservableTreeItem)) {
				return false;
			}
			if (!(((ObservableTreeItem) element).getModel() instanceof IResource)) {
				return false;
			}
			
			IResource resource = (IResource) ((ObservableTreeItem) element).getModel();
			if (resource instanceof IService) {
				return isMatching(filterText.getText(), (IService) resource);
			} else {
				return true;
			}
		}

		private boolean isMatching(String filter, IService service) {
			for (String label : service.getSelector().values()) {
				if (!StringUtils.isEmpty(label) 
						&& label.contains(filter)) {
					return true;
				}
			}
			return false;
		}
	}

	protected SelectionAdapter onClickCreateOrImport() {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
//				IWorkbenchWizard wizard = null;
//				if (openshiftProject != null) {
//					wizard = new ImportApplicationWizard();
//				} else {
//					wizard = new NewApplicationWizard();
//				}
//				if (WizardUtils.openWizardDialog(wizard, composite.getShell())) {
//					// Cancel this wizard, a server has already been created
//					// This is really ugly
//					closeWizard(callback);
//				} else {
//					projectsByOpenshiftProjects = createWorkspaceProjectsByOpenShiftProjects(openshiftProjects);
//					setDeployProjectCombo(openshiftProject, projectsByOpenshiftProjects);
//					updateCreateOrImportLink();
//					updateErrorMessage(null);
//				}
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
		WizardUtils.close(wizard);
	}

	private void updateCreateOrImportLink() {
//		boolean enabled = true;
//		if(application != null){
//			IProject[] p = ExpressServerUtils.findProjectsForApplication(application);
//			enabled = p == null || p.length == 0;
//			importLink.setText(ExpressUIMessages.OpenShiftServerWizardImportLink);
//		}else{
//			importLink.setText(ExpressUIMessages.OpenShiftServerWizardCreateLink);
//		}
//		importLink.setEnabled(enabled);
	}

//	private void updateErrorMessage(String message) {
//		callback.setErrorMessage(createErrorMessage(message));
//	}

//	public String createErrorMessage() {
//		return createErrorMessage(null);
//	}
	
//	public String createErrorMessage(String message) {
//		String error = null;
//		if (message != null) {
//			error = message;
//		} else if (connection == null) {
//			error = "Please select an existing connection or create a new one.";
//		} else if (openshiftProjects == null) {
//			error = NLS.bind("You have no project in connection to {0}. Please create a new one.", connection.getHost());
//		}
//		return error;
//	}

	protected com.openshift.restclient.model.IProject getOpenshiftProject(
			final com.openshift.restclient.model.IProject openshiftProject, final List<com.openshift.restclient.model.IProject> openshiftProjects) {
		if (openshiftProject == null) {
			return getFirstOpenshiftProject(openshiftProjects);
		} else if (openshiftProjects != null
				&& openshiftProjects.indexOf(openshiftProject) == -1) {
			// domain switched, current domain not contained within new list
			return getFirstOpenshiftProject(openshiftProjects);
		} else {
			return openshiftProject;
		}
	}

	private com.openshift.restclient.model.IProject getFirstOpenshiftProject(final List<com.openshift.restclient.model.IProject> openshiftProjects) {
		if (openshiftProjects != null
				&& openshiftProjects.size() > 0) {
			return openshiftProjects.get(0);
		}
		return null;
	}

	protected IBuildConfig getBuildConfig(final IBuildConfig buildConfig, final List<IBuildConfig> buildConfigs) {
		if (buildConfig == null) {
			return getFirstBuildConfig(buildConfigs);
		} else if (buildConfigs != null
				&& buildConfigs.indexOf(buildConfig) == -1){ 
			// project changed, build config not within list of build configs of new project
			return getFirstBuildConfig(buildConfigs);
		} else {
			return buildConfig;
		}
	}

	private IBuildConfig getFirstBuildConfig(List<IBuildConfig> buildConfigs) {
		IBuildConfig buildConfig = null; 
		if (buildConfigs != null
				&& buildConfigs.size() > 0) {
			buildConfig = buildConfigs.get(0);
		}
		return buildConfig;
	}

	private IProject getDeployProject(IBuildConfig buildConfig) {
//		if (openshiftProject == null) {
//			return null;
//		}
//		List<IProject> projects = OpenShiftServerUtils.getProjectsForBuildConfig(buildConfig);
//		if (projects == null
//				|| projects.size() < 1) {
//			return null;
//		}
//		
//		return projects.get(0);
		return null;
	}

//	private Map<com.openshift.restclient.model.IProject, IProject[]> createWorkspaceProjectsByOpenShiftProjects(List<com.openshift.restclient.model.IProject> openshiftProjects) {
//		Map<com.openshift.restclient.model.IProject, IProject[]> workspaceProjectsByOpenShiftProject = new HashMap<com.openshift.restclient.model.IProject, IProject[]>();
//		if (openshiftProjects != null) {
//			for (com.openshift.restclient.model.IProject project : openshiftProjects){
//				List<com.openshift.restclient.model.IResource> buildConfigs = project.getResources(ResourceKind.BUILD_CONFIG);
//				if (buildConfigs != null) {
//					Set<IProject> projects = new HashSet<>();
//					for (com.openshift.restclient.model.IResource resource : buildConfigs) {
//						if (resource instanceof IBuildConfig) {
//							projects.addAll(OpenShiftServerUtils.getProjectsForBuildConfig((IBuildConfig) resource));
//						}
//					}
//					workspaceProjectsByOpenShiftProject.put(openshiftProject, projects.toArray(new IProject[projects.size()]));
//				}
//			}
//		}
//		return workspaceProjectsByOpenShiftProject;
//	}

	public void performFinish(final IProgressMonitor monitor) throws CoreException {
		model.updateServer();
	}

//	private void configureServer(final com.openshift.restclient.model.IProject openshiftProject, IServerWorkingCopy server) throws OpenShiftException {
//		String serverName = OpenShiftServerUtils.getDefaultServerName(openshiftProject);
//		OpenShiftServerUtils.fillServerWithOpenShiftDetails(
//				server, serverName, deployProject, deployFolder, remote, openshiftProject, domain);
//		OpenShiftServerUtils.fillServerWithOpenShiftDetails(
//				serverName, null, null, openshiftProject, server);
//	}

	public DataBindingContext getDataBindingContext() {
		return this.dbc;
	}
}
