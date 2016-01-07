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

import java.lang.reflect.InvocationTargetException;

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
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
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
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport.IFormPresenter;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;

import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardFragment extends WizardHandleAwareFragment implements ICompletable {

private ServerSettingsViewModel model;

	private Control uiHook = null;

	public ServerSettingsWizardFragment() {
		// no finishing wizard before input provided in this page
		setComplete(false);
	}

	@Override
	public boolean hasComposite() {
		return true;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		handle.setTitle("Create an OpenShift 3 Server Adapter");
		handle.setDescription("Create an OpenShift 3 Server Adapter by selecting the project, service and folders used for file synchronization.");
		handle.setImageDescriptor(OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_MEDIUM);
		IServerWorkingCopy server = OpenShiftServerTaskModelAccessor.getServer(getTaskModel());
		Connection connection = OpenShiftServerTaskModelAccessor.getConnection(getTaskModel());
		this.model = new ServerSettingsViewModel(server, connection);
		DataBindingContext dbc = new DataBindingContext();
		Composite composite = createControls(parent, model, dbc);
		uiHook = composite.getChildren()[0];
		new FormPresenterSupport(
				new IFormPresenter() {

					@Override
					public void setMessage(String message, int type) {
						WizardFragmentUtils.getWizardPage(handle).setMessage(message, type);
					}

					@Override
					public void setComplete(boolean complete) {
						WizardFragmentUtils.getWizardPage(handle).setPageComplete(complete);
						ServerSettingsWizardFragment.this.setComplete(complete);
						handle.update();
					}

					@Override
					public Control getControl() {
						return parent;
					}
				}, 
			dbc);
		
		loadResources(model, WizardFragmentUtils.getWizardPage(handle).getWizard().getContainer());
		
		IProject selectedProject = UIUtils.getFirstSelectedWorkbenchProject();
		if (selectedProject != null) {
			model.setDeployProject(selectedProject);
		}

		return composite;
	}
	
	private Composite createControls(Composite parent, ServerSettingsViewModel model, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(4)
			.margins(10, 10)
			.applyTo(container);
		
		createProjectControls(container, model, dbc);
		createSourcePathControls(container, model, dbc);
		createDeploymentControls(container, model, dbc);
		createServiceControls(container, model, dbc);

		return container;
	}

	private void createProjectControls(Composite container, ServerSettingsViewModel model, DataBindingContext dbc) {
		Label projectLabel = new Label(container, SWT.NONE);
		projectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(projectLabel);

		StructuredViewer projectsViewer = new ComboViewer(container);
		GridDataFactory.fillDefaults()
			.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false)
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
		Button browseProjectsButton = new Button(container, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.hint(100, SWT.DEFAULT)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects(model, browseProjectsButton.getShell()));
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

	private void createSourcePathControls(Composite container, ServerSettingsViewModel model2,
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
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(sourcePathText))
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
			.to(BeanProperties.value(ServerSettingsViewModel.PROPERTY_SOURCE_PATH).observe(model))
			.in(dbc);
		
		Button browseSourceButton = new Button(container, SWT.PUSH);
		browseSourceButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(browseSourceButton);		
		browseSourceButton.addSelectionListener(onBrowseSource(browseSourceButton.getShell()));

		Button browseWorkspaceSourceButton = new Button(container, SWT.PUSH | SWT.READ_ONLY);
		browseWorkspaceSourceButton.setText("Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(browseWorkspaceSourceButton);		
		browseWorkspaceSourceButton.addSelectionListener(onBrowseWorkspace(browseWorkspaceSourceButton.getShell()));
	}
	
	private SelectionAdapter onBrowseSource(final Shell shell) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setText("Choose the source path to sync");
				dialog.setFilterPath(model.getSourcePath());
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
				DirectoryDialog dialog = new DirectoryDialog(shell, SWT.OPEN);
				dialog.setText("Choose the source path to sync");
				dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
				String filepath = dialog.open();
				if (!StringUtils.isEmpty(filepath)) {
					model.setSourcePath(filepath);
				}
			}
		};
	}

	private void createDeploymentControls(Composite container, ServerSettingsViewModel model, DataBindingContext dbc) {
		Label deployPathLabel = new Label(container, SWT.NONE);
		deployPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(deployPathLabel);

		Text deployPathText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
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
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_POD_PATH).observe(model))
			.in(dbc);
	}

	private void createServiceControls(Composite container, ServerSettingsViewModel model, DataBindingContext dbc) {
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
	}

	private IListChangeListener onServiceItemsChanged(final TreeViewer servicesViewer) {
		return new IListChangeListener() {
			
			@Override
			public void handleListChange(ListChangeEvent event) {
				servicesViewer.expandAll();
			}
		};
	}

	private TreeViewer createServicesTreeViewer(Composite parent, ServerSettingsViewModel model, Text selectorText) {
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

	/**
	 * Loads the resources for this view, does it in a blocking way.
	 * @param model, IWizardContainer container 
	 */
	private void loadResources(ServerSettingsViewModel model, IWizardContainer container) {
		try {
			WizardUtils.runInWizard(new Job("Loading projects and services...") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					model.loadResources();
					//initializing the project combo with the project selected in the workspace
					return Status.OK_STATUS;
				}
			}, container);
		} catch (InvocationTargetException | InterruptedException e) {
			// swallow intentionally
		}
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		model.updateServer();
		uiHook = null;
	}

	@Override
	public void performCancel(IProgressMonitor monitor) throws CoreException {
		uiHook = null;
	}

	@Override
	public void setComplete(boolean complete) {
		super.setComplete(complete);
	}

	@Override
	public boolean isComplete() {
		return uiHook != null && !uiHook.isDisposed() && super.isComplete();
	}

}
