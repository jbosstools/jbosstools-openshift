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

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.BooleanUtils;
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
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.SelectProjectComponentBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport.IFormPresenter;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.DialogAdvancedPart;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.dialog.SelectRouteDialog.RouteLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardPage extends AbstractOpenShiftWizardPage implements ICompletable {
	static final String IS_LOADING_SERVICES = "isLoadingServices";

	protected ServerSettingsWizardPageModel model;
	protected boolean needsLoadingResources = true;
	protected boolean isLoadingResources = false;
	protected Control uiHook = null;

	/**
	 * Invoked from new server wizard (servers view, main menu)
	 * 
	 * @param wizard the parent {@link IWizard} 
	 * @param connection the current OpenShift {@link Connection}
	 */
	public ServerSettingsWizardPage(final IWizard wizard, final IServerWorkingCopy server, final Connection connection, IProject deployProject) {
		this(wizard, server, connection, null, null, deployProject);
	}

	
	
	/**
	 * Invoked from OpenShift explorer
	 * 
	 * @param wizard the parent {@link IWizard} 
	 * @param server the working copy of the {@link IServer} to create
	 * @param connection the current OpenShift {@link Connection}
	 * @param service the selected service
	 */
	protected ServerSettingsWizardPage(final IWizard wizard, final IServerWorkingCopy server, final Connection connection, 
			final IService service, final IRoute route) {
		this(wizard, server, connection, service, route, null);
	}

	protected ServerSettingsWizardPage(final IWizard wizard, final IServerWorkingCopy server, final Connection connection, 
			final IService service, final IRoute route, final IProject deployProject) {
		super("Server Settings", 
				"Create an OpenShift 3 Server Adapter by selecting the project, service and folders used for file synchronization.", 
				"Create an OpenShift 3 Server Adapter", 
				wizard);
		this.model = new ServerSettingsWizardPageModel(service, route, deployProject, connection, server);
	}
	
	/**
	 * @return the {@link ServerSettingsWizardPageModel} associated with this page.
	 */
	ServerSettingsWizardPageModel getModel() {
		return model;
	}
	
	void updateServer() throws OpenShiftException {
		model.updateServer();
	}
	
	/**
	 * @return a boolean flag to indicate if this page needs to load resources from OpenShift.
	 */
	public boolean isNeedsLoadingResources() {
		return needsLoadingResources;
	}
	
	/**
	 * @return a boolean flag to serverSettingsWizardPageindicate if this page is currently loading resources from OpenShift.
	 */
	public boolean isLoadingResources() {
		return isLoadingResources;
	}
	
	@Override
	public void setComplete(final boolean complete) {
		setPageComplete(complete);
	}
	
	@Override
	protected void doCreateControls(final Composite parent, final DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(0, 0).applyTo(parent);
		createControls(parent, model, dbc);
		isLoadingResources = false; //Since wizard fragment is cached and reused, this precaution is needed.
		new FormPresenterSupport(
				new IFormPresenter() {

					@Override
					public void setMessage(String message, int type) {
						ServerSettingsWizardPage.this.setMessage(message, type);
					}

					@Override
					public void setComplete(boolean complete) {
						ServerSettingsWizardPage.this.setComplete(complete);
					}

					@Override
					public Control getControl() {
						return parent;
					}
				}, 
			dbc);
		
		// assuming that the wizard may be complete upon initialization 
		setComplete(true);
		loadResources(getContainer());		
	}
	
	/**
	 * Loads the resources for this view, does it in a blocking way.
	 * @param model
	 * @param container 
	 */
	private void loadResources(final IWizardContainer container) {
		try {
			WizardUtils.runInWizard(new Job("Loading projects and services...") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					ServerSettingsWizardPage.this.model.loadResources();
					ServerSettingsWizardPage.this.needsLoadingResources = false;
					return Status.OK_STATUS;
				}
			}, container);
		} catch (InvocationTargetException | InterruptedException e) {
			// swallow intentionally
		}
	}

	/**
	 * Sets the default deployment project in the wizard page, unless it is <code>null</code>
	 * @param project the project to select
	 */
	void setDeploymentProject(final IProject project) {
		if (project != null) {
			model.setDeployProject(project);
		}
	}
	
	private final int numColumns = 4;
	
	private Composite createControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(numColumns)
			.margins(6, 6)
			.applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		createProjectControls(container, model, dbc);
		createServiceControls(container, model, dbc);
		createAdvancedGroup(container, dbc);
		uiHook = container;
		return container;
	}

	private void createAdvancedGroup(Composite parent, DataBindingContext dbc) {
		DialogAdvancedPart advancedPart = new DialogAdvancedPart() {
			
			@Override
			protected void createAdvancedContent(Composite advancedComposite) {
				createSourcePathControls(advancedComposite, model, dbc);
				createDeploymentControls(advancedComposite, model, dbc);
				createRouteControls(advancedComposite, model, dbc);
			}

			@Override
			protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
				return gridLayoutFactory.numColumns(numColumns).margins(0, 0);
			}
		};
		advancedPart.createAdvancedGroup(parent, numColumns);
	}
	
	private void createProjectControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		IObservableValue<IProject> eclipseProjectObservable = BeanProperties.value(
				ServerSettingsWizardPageModel.PROPERTY_DEPLOYPROJECT).observe(model);
		new SelectProjectComponentBuilder()
			.setTextLabel("Eclipse Project: ")
			.setHorisontalSpan(2)
			.setEclipseProjectObservable(eclipseProjectObservable)
			.setSelectionListener(onBrowseProjects(model, container.getShell()))
			.build(container, dbc);
	}

	/**
	 * Open a dialog box to select an open project when clicking on the 'Browse' button.
	 * 
	 * @return
	 */
	private SelectionListener onBrowseProjects(ServerSettingsWizardPageModel model, final Shell shell) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectExistingProjectDialog dialog = 
						new SelectExistingProjectDialog("Select a project to deploy", shell);
				if(model.getDeployProject() != null) {
					dialog.setInitialSelections(new Object[]{model.getDeployProject()});
				}
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					if (selectedProject instanceof IProject) {
						model.setDeployProject((org.eclipse.core.resources.IProject) selectedProject);
					}
				}
			}
		};
	}

	private void createSourcePathControls(Composite container, ServerSettingsWizardPageModel model,
			DataBindingContext dbc) {
		Label sourcePathLabel = new Label(container, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(sourcePathLabel);

		Text sourcePathText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(sourcePathText);
		Binding sourcePathBinding = ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(sourcePathText))
			.validatingAfterConvert(new IValidator() {

				@Override
				public IStatus validate(Object value) {
					String path = (String) value;
					if (StringUtils.isEmpty(path)) {
						return ValidationStatus.cancel("Please provide a local path to deploy from.");
					}
					String provideValidPathMessage = "Please provide a valid local path to deploy from.";
					try {
						path = VariablesHelper.replaceVariables(path);
					} catch (OpenShiftCoreException e) {
						String message = org.apache.commons.lang.StringUtils.substringAfter(e.getMessage(), "Exception:");
						return ValidationStatus.error(provideValidPathMessage + "\nError: " + message);
					}
					if (!isReadableFile(path)) {
						return ValidationStatus.error(provideValidPathMessage);
					}
					return ValidationStatus.ok();
				}

				private boolean isReadableFile(String path) {
					return new File(path).canRead();
				}
				
			})
			.to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_SOURCE_PATH).observe(model))
			.in(dbc);
		ControlDecorationSupport.create(
				sourcePathBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		Button browseSourceButton = new Button(container, SWT.PUSH);
		browseSourceButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(browseSourceButton);		
		browseSourceButton.addSelectionListener(onBrowseSource(browseSourceButton.getShell()));

		Button browseWorkspaceSourceButton = new Button(container, SWT.PUSH | SWT.READ_ONLY);
		browseWorkspaceSourceButton.setText("Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(browseWorkspaceSourceButton);		
		browseWorkspaceSourceButton.addSelectionListener(onBrowseWorkspace(browseWorkspaceSourceButton.getShell()));

		UIUtils.setEqualButtonWidth(browseSourceButton, browseWorkspaceSourceButton);
	}
	
	private SelectionAdapter onBrowseSource(final Shell shell) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setText("Choose the source path to sync");
				String sourcePath = VariablesHelper.replaceVariables(model.getSourcePath(), true);
				dialog.setFilterPath(sourcePath);
				String filepath = dialog.open();
				if (!StringUtils.isEmpty(filepath)) {
					model.setSourcePath(filepath);
				}
			}
		};
	}

	private SelectionAdapter onBrowseWorkspace(final Shell shell) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String sourcePath = getWorkspaceRelativePath(model.getSourcePath());
				ElementTreeSelectionDialog dialog = createWorkspaceFolderDialog(shell, sourcePath);
				if (dialog.open() == IDialogConstants.OK_ID 
						&& dialog.getFirstResult() instanceof IContainer) {
					String path = ((IContainer) dialog.getFirstResult()).getFullPath().toString();
					String folderPath = VariablesHelper.addWorkspacePrefix(path);
					model.setSourcePath(folderPath);
				}
			}
		};
	}

	private String getWorkspaceRelativePath(String sourcePath) {
		if (org.apache.commons.lang.StringUtils.isBlank(sourcePath) || sourcePath.contains("{")) {
			return sourcePath;
		}
		IPath absolutePath = new Path(sourcePath);
		IContainer container = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(absolutePath);
		if (container != null) {
			return container.getFullPath().toString();
		}
		return null;
	}

	private ElementTreeSelectionDialog createWorkspaceFolderDialog(Shell shell, String selectedFile) {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				shell,
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider()
		);
		dialog.setTitle("Select a workspace folder");
		dialog.setMessage("Select a workspace folder to deploy");
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (!(element instanceof IContainer)) {
					return false;
				}
				IContainer container = (IContainer) element;
				return container.isAccessible()
						&& !ProjectUtils.isInternalPde(container.getName())
						&& !ProjectUtils.isInternalRSE(container.getName());
			}
		});
		dialog.setAllowMultiple(false);
		org.eclipse.core.resources.IResource res = model.getDeployProject();
		if (org.apache.commons.lang.StringUtils.isNotBlank(selectedFile)) {
			String path = VariablesHelper.getWorkspacePath(selectedFile);
			org.eclipse.core.resources.IResource member = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (member != null) {
				res = member;
			}
		}
		if (res != null) {
			dialog.setInitialSelection(res);
		}

		return dialog;
	}

	private void createDeploymentControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Button useInferredPodPathButton = new Button(container, SWT.CHECK);
		useInferredPodPathButton.setText("&Use inferred Pod Deployment Path");
		GridDataFactory.fillDefaults()
			.span(4,1).align(SWT.FILL, SWT.CENTER)
			.applyTo(useInferredPodPathButton);
		ISWTObservableValue useInferredPodPathObservable = WidgetProperties.selection().observe(useInferredPodPathButton);
		ValueBindingBuilder
				.bind(useInferredPodPathObservable)
				.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_INFERRED_POD_PATH).observe(model))
				.in(dbc);
		
		Label podPathLabel = new Label(container, SWT.NONE);
		podPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(podPathLabel);

		Text podPathText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(podPathText);
		ISWTObservableValue podPathObservable = WidgetProperties.text(SWT.Modify).observe(podPathText);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(podPathText))
			.notUpdatingParticipant()
			.to(useInferredPodPathObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(podPathLabel))
			.notUpdatingParticipant()
			.to(useInferredPodPathObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(podPathObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_POD_PATH).observe(model))
			.in(dbc);
		PodPathValidator podPathValidator = new PodPathValidator(useInferredPodPathObservable, podPathObservable);
		ControlDecorationSupport.create(
				podPathValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		dbc.addValidationStatusProvider(podPathValidator);
	}

	private static class PodPathValidator extends MultiValidator {
		
		private IObservableValue<Boolean> useDefaultPodPath;
		private IObservableValue<String> podPath;

		public PodPathValidator(IObservableValue<Boolean> useDefaultPodPath, IObservableValue<String> podPath) {
			this.useDefaultPodPath = useDefaultPodPath;
			this.podPath = podPath;
		}

		@Override
		protected IStatus validate() {
			if (BooleanUtils.isFalse((Boolean) useDefaultPodPath.getValue())) {
				if (StringUtils.isEmpty(podPath.getValue())) {
					return ValidationStatus.cancel("Please provide a path to deploy to on the pod.");
				}
				if (!Path.isValidPosixPath((String) podPath.getValue())) {
					return ValidationStatus.error("You have to choose a path on the pod that route that will be used for this server adapter.");
				}
			}
			return ValidationStatus.ok();
		}
		
	}
	
	private void createServiceControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group servicesGroup = new Group(container, SWT.NONE);
		servicesGroup.setText("Services");
		GridDataFactory.fillDefaults()
			.span(4, 1).align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(servicesGroup);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10,10)
			.applyTo(servicesGroup);

		Label selectorLabel = new Label(servicesGroup, SWT.NONE);
		selectorLabel.setText("Selector:");
		Text selectorText = UIUtils.createSearchText(servicesGroup);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(selectorText);

		final TreeViewer servicesViewer = createServicesTreeViewer(servicesGroup, model, selectorText);
		BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_SERVICE_ITEMS).observe(model)
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
						}
						return ValidationStatus.ok();
						
					}
				})
				.to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_SERVICE).observe(model))
				.converting(new Model2ObservableTreeItemConverter(new ServerSettingsWizardPageModel.ServiceTreeItemsFactory()))
				.in(dbc);

		// details
		ExpandableComposite expandable = new ExpandableComposite(servicesGroup, SWT.None);
		GridDataFactory.fillDefaults()
			.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
			.applyTo(expandable);
		expandable.setText("Service Details");
		expandable.setExpanded(true);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(0, 0).applyTo(expandable);
		GridDataFactory.fillDefaults()
		.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
		.applyTo(expandable);
		
		Composite detailsContainer = new Composite(expandable, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).hint(SWT.DEFAULT, 150)
				.applyTo(detailsContainer);
		IObservableValue<IResource> selectedService = new WritableValue<IResource>();
		ValueBindingBuilder
			.bind(selectedServiceTreeItem)
			.converting(new ObservableTreeItem2ModelConverter())
			.to(selectedService)
			.notUpdatingParticipant()
			.in(dbc);
		new ServiceDetailViews(selectedService, detailsContainer, dbc).createControls();
		
		expandable.setClient(detailsContainer);
		expandable.addExpansionListener(new IExpansionListener() {
			@Override
			public void expansionStateChanging(ExpansionEvent e) {
			}
			
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				servicesGroup.update();
				servicesGroup.layout(true);
			}
		});
		
	}

	private void createRouteControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group routeGroup = new Group(container, SWT.NONE);
		routeGroup.setText("Route");
		GridDataFactory.fillDefaults()
			.span(4, 1).align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(routeGroup);
		GridLayoutFactory.fillDefaults()
			.applyTo(routeGroup);

		// additional nesting required because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
		Composite routeContainer = new Composite(routeGroup, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(routeContainer);
		GridLayoutFactory.fillDefaults()
			.margins(10,10).numColumns(2)
			.applyTo(routeContainer);
		
		Button promptRouteButton = new Button(routeContainer, SWT.CHECK);
		promptRouteButton.setSelection(true);
		promptRouteButton.setText("Prompt for route when multiple routes available to show in browser");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(promptRouteButton);

		Label routeLabel = new Label(routeContainer, SWT.NONE);
		routeLabel.setText("Use Route: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(routeLabel);

		StructuredViewer routesViewer = new ComboViewer(routeContainer);
		GridDataFactory.fillDefaults()
			.span(1,1).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(routesViewer.getControl());
		routesViewer.setContentProvider(new ObservableListContentProvider());
		routesViewer.setLabelProvider(new RouteLabelProvider());
		routesViewer.setInput(
				BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_ROUTES).observe(model));
//		routesViewer.setComparer(new IElementComparer() {
//
//			@Override
//			public boolean equals(Object object1, Object object2) {
//				if (object1 instanceof IRoute) {
//					if (!(object2 instanceof IRoute)) {
//						return false;
//					}
//
//					IRoute route1 = (IRoute) object1;
//					IRoute route2 = (IRoute) object2;
//
//					return Objects.equals(route1.getServiceName(), route2.getServiceName()) 
//							&& Objects.equals(route1.getURL(), route2.getURL());
//				} else if (object2 instanceof IRoute) {
//					return false;
//				} else {
//					return Objects.equals(object1, object2);
//				}
//			}
//
//			@Override
//			public int hashCode(Object element) {
//				if (element instanceof IRoute) {
//					IRoute route = (IRoute) element;
//					return new HashCodeBuilder()
//							.append(route.getServiceName())
//							.append(route.getURL())
//							.toHashCode();
//				}
//				return element.hashCode();
//			}
//		});
		
		IObservableValue<IResource> selectedRouteObservable = ViewerProperties.singleSelection().observe(routesViewer);
		ValueBindingBuilder
			.bind(selectedRouteObservable)
			.to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_ROUTE).observe(model))
			.in(dbc);

		final IObservableValue<Boolean> isSelectDefaultRouteObservable =
				WidgetProperties.selection().observe(promptRouteButton);
		final IObservableValue<Boolean> selectDefaultRouteModelObservable = 
				BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_SELECT_DEFAULT_ROUTE).observe(model);
		ValueBindingBuilder
			.bind(isSelectDefaultRouteObservable)
			.converting(new InvertingBooleanConverter())
			.to(selectDefaultRouteModelObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(routesViewer.getControl()))
			.notUpdating(selectDefaultRouteModelObservable)
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(routeLabel))
			.notUpdating(selectDefaultRouteModelObservable)
			.in(dbc);
		RouteValidator routeValidator = new RouteValidator(isSelectDefaultRouteObservable, selectedRouteObservable);
		dbc.addValidationStatusProvider(routeValidator);
		ControlDecorationSupport.create(routeValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}

	private IListChangeListener<ObservableTreeItem> onServiceItemsChanged(final TreeViewer servicesViewer) {
		return new IListChangeListener<ObservableTreeItem>() {

			@Override
			public void handleListChange(ListChangeEvent<? extends ObservableTreeItem> event) {
				servicesViewer.expandAll();
			}
			
		};
	}

	private TreeViewer createServicesTreeViewer(Composite parent, ServerSettingsWizardPageModel model, Text selectorText) {
		TreeViewer applicationTemplatesViewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty<ServerSettingsWizardPageModel, ObservableTreeItem> childrenProperty = new MultiListProperty<ServerSettingsWizardPageModel, ObservableTreeItem>(
				new IListProperty[] {
						BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_SERVICE_ITEMS),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		applicationTemplatesViewer.setContentProvider(contentProvider);
		applicationTemplatesViewer.setLabelProvider(new ServicesViewLabelProvider());
		applicationTemplatesViewer.addFilter(new ServiceViewerFilter(selectorText));
		applicationTemplatesViewer.setComparator(ProjectViewerComparator.createProjectTreeSorter());
		applicationTemplatesViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		applicationTemplatesViewer.setInput(model);
		return applicationTemplatesViewer;
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

	@Override
	public void dispose() {
		super.dispose();
		uiHook = null;
		//not good set model to null here
	}

    @Override
	public boolean isPageComplete() {
        return !isLoadingResources 
        		&& uiHook != null && !uiHook.isDisposed() 
    			&& !needsLoadingResources 
    			&& model != null && model.getService() != null 
    			&& super.isPageComplete();
    }

    public IServer saveServer(IProgressMonitor monitor) throws CoreException {
    	model.updateServer();
		return model.saveServer(monitor);
	}

	class RouteValidator extends MultiValidator {

		private IObservableValue<Boolean> useDefaultRoute;
		private IObservableValue<IResource> selectedRoute;

		public RouteValidator(IObservableValue<Boolean> useDefaultRoute, IObservableValue<IResource> selectedRoute) {
			this.useDefaultRoute = useDefaultRoute;
			this.selectedRoute = selectedRoute;
		}

		@Override
		protected IStatus validate() {
			if (BooleanUtils.isFalse((Boolean) useDefaultRoute.getValue())) {
				if (selectedRoute.getValue() == null) {
					return ValidationStatus.cancel("You have to choose a route that will be used for this server adapter.");
				}
			}
			return ValidationStatus.ok();
		}
		
	}
}
