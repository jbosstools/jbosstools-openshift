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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.job.DisableAllWidgetsJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftServerEditorSection extends ServerEditorSection {

	private IServerEditorPartInput input;
	private OpenShiftServerEditorModel model;

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		if (input instanceof IServerEditorPartInput) {
			this.input = (IServerEditorPartInput) input;
		}
	}

	@Override
	public void createSection(Composite parent) {
		super.createSection(parent);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent,
				ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR);
		section.setText("OpenShift Server Adapter");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(section);

		this.model = new OpenShiftServerEditorModel(server, null);

		Composite container = createControls(section, model);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(container);
		toolkit.paintBordersFor(container);
		toolkit.adapt(container);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(container);
		
		section.setClient(container);

		loadResources(section, model);
	}

	private Composite createControls(Composite parent, OpenShiftServerEditorModel model) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()
				.numColumns(3)
				.applyTo(container);

		DataBindingContext dbc = new DataBindingContext();
		
		// connection
		createConnectionContols(container, dbc);
		
		// project settings
		Group projectSettingGroup = new Group(container, SWT.NONE);
		projectSettingGroup.setText("Project Settings:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).span(3, 1)
			.applyTo(projectSettingGroup);
		GridLayoutFactory.fillDefaults()
				.margins(10, 10)
				.applyTo(projectSettingGroup);

		// additional nesting required because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
		Composite projectSettingsContainer = new Composite(projectSettingGroup, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(projectSettingsContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(4)
			.applyTo(projectSettingsContainer);
		
		createProjectControls(projectSettingsContainer, dbc);
		createDeploymentControls(projectSettingsContainer, dbc);
		createSourcePathControls(projectSettingsContainer, dbc);
		createServiceControls(projectSettingsContainer, dbc);

		return container;
	}

	private void createConnectionContols(Composite parent, DataBindingContext dbc) {
		Label connectionLabel = new Label(parent, SWT.NONE);
		connectionLabel.setText("Connection:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.applyTo(connectionLabel);

		Combo connectionCombo = new Combo(parent, SWT.DEFAULT);
		ComboViewer connectionViewer = new ComboViewer(connectionCombo);
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ConnectionColumLabelProvider());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false)
				.applyTo(connectionCombo);
		connectionViewer.setInput(
				BeanProperties.list(OpenShiftServerEditorModel.PROPERTY_CONNECTIONS).observe(model));
		 Binding connectionBinding = ValueBindingBuilder	
			.bind(ViewerProperties.singleSelection().observe(connectionViewer))
			.validatingAfterGet(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (!(value instanceof Connection)) {
						return ValidationStatus.cancel("Please select a connection for this server adapter.");
					}
					return ValidationStatus.ok();
				}
			})
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_CONNECTION).observe(model))
		 	.in(dbc);
		ControlDecorationSupport.create(
				connectionBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Button newConnectionButton = new Button(parent, SWT.PUSH);
		newConnectionButton.setText("New...");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT)
			.applyTo(newConnectionButton);
		newConnectionButton.addSelectionListener(onNewConnection(connectionViewer));
	}

	private SelectionListener onNewConnection(ComboViewer connectionViewer) {
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

	private void createProjectControls(Composite parent, DataBindingContext dbc) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(projectLabel);

		StructuredViewer projectsViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
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
				BeanProperties.list(OpenShiftServerEditorModel.PROPERTY_PROJECTS).observe(model));

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
					.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEPLOYPROJECT)
					.observe(model))
					.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		// browse projects
		Button browseProjectsButton = new Button(parent, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects(model, browseProjectsButton.getShell()));
	}

	private SelectionListener onBrowseProjects(OpenShiftServerEditorModel model, final Shell shell) {
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

	private void createSourcePathControls(Composite container, DataBindingContext dbc) {
		Label sourcePathLabel = new Label(container, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(sourcePathLabel);

		Text sourcePathText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(sourcePathText);
		ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(sourcePathText))
				.validatingAfterConvert(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof String) || StringUtils.isEmpty(value)) {
							return ValidationStatus.cancel("Please provide a source path to deploy to the pod.");
						}
						if (!isValidFile((String) value)) {
							return ValidationStatus.error("Please provide a valid path to deploy to the pod");
						}
						return ValidationStatus.ok();
					}
					
					private boolean isValidFile(String path) {
						return FileUtils.exists(new File(path));
					}

				})
				.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_SOURCE_PATH).observe(model))
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

	private void createDeploymentControls(Composite parent, DataBindingContext dbc) {
		Label deployPathLabel = new Label(parent, SWT.NONE);
		deployPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(deployPathLabel);

		Text deployPathText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(deployPathText);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(deployPathText))
			.validatingAfterConvert(new IValidator() {

				@Override
				public IStatus validate(Object value) {
					if (!(value instanceof String) || StringUtils.isEmpty(value)) {
						return ValidationStatus.cancel("Please provide a path to deploy to on the pod.");
					}
					if (!Path.isValidPosixPath((String)value)) {
						return ValidationStatus.error("Please provide a valid path to deploy to on the pod");
					}
					return ValidationStatus.ok();
				}
				
			})
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_POD_PATH).observe(model))
			.in(dbc);
	}

	private void createServiceControls(Composite parent, DataBindingContext dbc) {
		Label serviceLabel = new Label(parent, SWT.NONE);
		serviceLabel.setText("Service:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(serviceLabel);

		Text serviceText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
			.span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(serviceText);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(serviceText))
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_SERVICE).observe(model))
			.converting(new Converter(IService.class, String.class) {
				
				@Override
				public Object convert(Object fromObject) {
					if (!(fromObject instanceof IService)) {
						return null;
					};
					return ((IService) fromObject).getName();
				}
			})
			.in(dbc);

		Button selectServiceButton = new Button(parent, SWT.PUSH);
		selectServiceButton.setText("Select...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(selectServiceButton);
		selectServiceButton.addSelectionListener(onSelectService(model, selectServiceButton.getShell()));
	}

	private SelectionListener onSelectService(OpenShiftServerEditorModel model, final Shell shell) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				
				SelectServiceWizard selectServiceDialog = 
						new SelectServiceWizard(NLS.bind("Select a service that your server adapter {0} will publish to.", 
								input.getServer().getName()), model.getService(), model.getConnection());
				if (WizardUtils.openWizardDialog(selectServiceDialog, shell) 
						== Dialog.OK) {
					model.setService(selectServiceDialog.getService());
				}
			}
		};
	}

	private void loadResources(Composite container, OpenShiftServerEditorModel model) {
		IServerWorkingCopy server = input.getServer();

		final Connection connection = OpenShiftServerUtils.getConnection(server);
		final IProject deployProject = OpenShiftServerUtils.getDeployProject(server);

		Cursor busyCursor = new Cursor(container.getDisplay(), SWT.CURSOR_WAIT);
		new JobChainBuilder(
				new DisableAllWidgetsJob(true, container, busyCursor))
				.runWhenDone(
						new Job("Loading projects...") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								model.loadResources();
								return Status.OK_STATUS;
							}
						})
				.runWhenDone(
						new Job("Setting connection, deploy project...") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								if (deployProject != null) {
									model.setDeployProject(deployProject);
								}
								if (connection != null) {
									model.setConnection(connection);
								}
								model.setService(OpenShiftServerUtils.getService(server));
								return Status.OK_STATUS;
							}
						})
				// disable widgets for now, we're not supporting editing yet
				.runWhenDone(new DisableAllWidgetsJob(true, container, false, busyCursor))
				.schedule();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		super.doSave(monitor);
	}
}
