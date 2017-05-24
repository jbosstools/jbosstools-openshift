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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.wst.server.core.IServerAttributes;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.connection.ConnectionURL;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionColumLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizard;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

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
	public void setServerEditorPart(ServerEditorPart editor) {
		super.setServerEditorPart(editor);
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

		this.model = new OpenShiftServerEditorModel(server, this, null);

		Composite container = createControls(section);
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

	private Composite createControls(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		container.setLayout(gl);
		
		DataBindingContext dbc = new DataBindingContext();
		
		// connection
		createConnectionContols(container, dbc);
		createProjectSettingsGroup(container, dbc);
		return container;
	}
	
	protected void createProjectSettingsGroup(Composite container, DataBindingContext dbc) {
		// project settings
		Group projectSettingGroup = new Group(container, SWT.SHADOW_NONE);
		projectSettingGroup.setText("Project Settings");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).span(3, 1)
			.applyTo(projectSettingGroup);
		GridLayoutFactory.fillDefaults()
				.applyTo(projectSettingGroup);

		// additional nesting required because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
		Composite projectSettingsContainer = new Composite(projectSettingGroup, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(projectSettingsContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(4)
			.margins(10, 10)
			.applyTo(projectSettingsContainer);
		
		createProjectControls(projectSettingsContainer, dbc);
		createDeploymentControls(projectSettingsContainer, dbc);
		createSourcePathControls(projectSettingsContainer, dbc);
		createResourceControls(projectSettingsContainer, dbc);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void createConnectionContols(Composite parent, DataBindingContext dbc) {
		Label connectionLabel = new Label(parent, SWT.NONE);
		connectionLabel.setText("Connection:");
		
		Composite comboHolder = new Composite(parent, SWT.NONE);
		comboHolder.setLayout(new FillLayout());
		GridData holderData = GridDataFactory.fillDefaults().grab(true,  false).create();
		holderData.widthHint = 300;
		comboHolder.setLayoutData(holderData);
		
		Combo connectionCombo = new Combo(comboHolder, SWT.DEFAULT);
		ComboViewer connectionViewer = new ComboViewer(connectionCombo);
		connectionViewer.setContentProvider(new ObservableListContentProvider());
		connectionViewer.setLabelProvider(new ConnectionColumLabelProvider());
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
		 
		BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_CONNECTION).observe(model)
				.addValueChangeListener(new IValueChangeListener() {
					@Override
					public void handleValueChange(ValueChangeEvent event) {
						new Job("Refresh connection details") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									model.loadResources();
								} catch (OpenShiftException oce) {
									OpenShiftUIActivator.log(IStatus.ERROR, "Error refreshing connection details", oce);
								}
								return Status.OK_STATUS;
							}

						}.schedule();
					}
				});
		ControlDecorationSupport.create(connectionBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));

		Button newConnectionButton = new Button(parent, SWT.PUSH);
		newConnectionButton.setText("New...");
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
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(projectLabel);

		Combo projectsCombo = new Combo(parent, SWT.DEFAULT);
		StructuredViewer projectsViewer = new ComboViewer(projectsCombo);
		GridDataFactory.fillDefaults()
			.span(2, 1)
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, true)
			.applyTo(projectsCombo);
		
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
					.validatingAfterConvert(value -> {
							if (value instanceof IProject) {
								return ValidationStatus.ok();
							}
							return ValidationStatus.cancel("Please choose a project to deploy.");
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
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects(model, browseProjectsButton.getShell()));

		UIUtils.ensureGTK3CombosAreCorrectSize(parent);
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

	private void createSourcePathControls(Composite parent, DataBindingContext dbc) {
		Label sourcePathLabel = new Label(parent, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(sourcePathLabel);

		Text sourcePathText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.span(3,1)
				.align(SWT.FILL, SWT.FILL)
				.grab(true, false)
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

		// filler
		new Label(parent, SWT.NONE);
		new Label(parent, SWT.NONE);

		Button browseSourceButton = new Button(parent, SWT.PUSH);
		browseSourceButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.applyTo(browseSourceButton);
		browseSourceButton.addSelectionListener(onBrowseSource(browseSourceButton.getShell()));

		Button browseWorkspaceSourceButton = new Button(parent, SWT.PUSH | SWT.READ_ONLY);
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
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(deployPathLabel);

		Text deployPathText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
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

	private void createResourceControls(Composite parent, DataBindingContext dbc) {
		Label kindLabel = new Label(parent, SWT.None);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(kindLabel);
		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(kindLabel))
			.notUpdatingParticipant()
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_RESOURCE).observe(model))
			.converting(new Converter(IResource.class, String.class) {

				@Override
				public Object convert(Object fromObject) {
					if (!(fromObject instanceof IResource)) {
						return "Resource:";
					}
					return ((IResource) fromObject).getKind() + ":";
				}
			})
			.in(dbc);

		Text resourceText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
        GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1)
			.grab(true, false)
			.applyTo(resourceText);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(resourceText))
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_RESOURCE).observe(model))
			.converting(new Converter(IResource.class, String.class) {
				
				@Override
				public Object convert(Object fromObject) {
					if (!(fromObject instanceof IResource)) {
						return "<not found>";
					};
					IResource resource = (IResource) fromObject;
					return resource.getNamespace() + "/" + resource.getName();
				}
			})
			.in(dbc);

		Button selectResourceButton = new Button(parent, SWT.PUSH);
		selectResourceButton.setText("Select...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(selectResourceButton);
		selectResourceButton.addSelectionListener(onSelectResource(model, selectResourceButton.getShell()));
	}

	private SelectionListener onSelectResource(OpenShiftServerEditorModel model, final Shell shell) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				
				SelectResourceWizard selectResourceDialog = 
						new SelectResourceWizard(NLS.bind("Select a resource that your server adapter {0} will publish to.", 
								input.getServer().getName()), model.getResource(), model.getConnection());
				if (WizardUtils.openWizardDialog(selectResourceDialog, shell) 
						== Dialog.OK) {
					model.setResource(selectResourceDialog.getResource());
				}
			}
		};
	}
	
	private class LoadingProjectsJob extends Job {
		private OpenShiftServerEditorModel model;
		private IServerWorkingCopy server;
		private IProject deployProject;
		public LoadingProjectsJob(OpenShiftServerEditorModel model, IServerWorkingCopy server, IProject deployProject) {
			super("Loading OpenShift Server Details...");
			this.model = model;
			this.server = server;
			this.deployProject = deployProject;
		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			model.setInitializing(true);
			String url = OpenShiftServerUtils.getConnectionURL(server);
			IConnection con = null;
			try {
				if( url != null ) {
					ConnectionURL conUrl = ConnectionURL.forURL(url);
					con = ConnectionsRegistrySingleton.getInstance().getByUrl(conUrl);
					model.loadResources(con);
				}
			} catch(UnsupportedEncodingException | MalformedURLException | OpenShiftException e) {
				IStatus s = OpenShiftUIActivator.statusFactory().errorStatus(getConnectionErrorMessage(url, server), e);
				OpenShiftUIActivator.getDefault().getLogger().logStatus(s);
			}
			
			if (deployProject != null) {
				model.setDeployProject(deployProject);
			}
			String sourcePath = OpenShiftServerUtils.getSourcePath(server);
			if (!StringUtils.isEmpty(sourcePath)) {
				model.setSourcePath(sourcePath);
			}
			String podPath = OpenShiftServerUtils.getPodPath(server);
			if(!StringUtils.isEmpty(podPath)) {
				model.setPodPath(podPath);
			}
			
			if( con != null ) {
				model.setConnection(con);
				try {
					// Do service last, since it's most likely to error
					model.setResource(OpenShiftServerUtils.getResource(server));
				} finally {
					model.setInitializing(false);
				}
			}			
			return Status.OK_STATUS;
		}
		
		private String getConnectionErrorMessage(String url, IServerAttributes server) {
			ConnectionURL connectionUrl = ConnectionURL.safeForURL(OpenShiftServerUtils.getConnectionURL(server));
			if (connectionUrl == null) {
				return "Could not find OpenShift connection for server \"{0}\"";
			} else {
				return NLS.bind("Could not find OpenShift connection to host \"{0}\" with user \"{1}\" for server \"{2}\"", 
					new String[] { connectionUrl.getHost(), connectionUrl.getUsername(), server.getName() } );
			}
		}
	}
	
	private Job loadingProjectsJob(OpenShiftServerEditorModel model, IServerWorkingCopy server, IProject project) {
		return new LoadingProjectsJob(model, server, project);
	}
	
	private void loadResources(final Composite container, OpenShiftServerEditorModel model) {
		IServerWorkingCopy server = input.getServer();
		final IProject deployProject = OpenShiftServerUtils.getDeployProject(server);

		Cursor busyCursor = new Cursor(container.getDisplay(), SWT.CURSOR_WAIT);
		IProgressMonitor chainProgressMonitor = new NullProgressMonitor() {
			@Override
			public boolean isCanceled() {
				return container.isDisposed();
			}
		};
		new JobChainBuilder(
				new DisableAllWidgetsJob(true, container, busyCursor), chainProgressMonitor)
				.runWhenDone(loadingProjectsJob(model, server, deployProject))
				.runWhenDone(new DisableAllWidgetsJob(false, container, false, busyCursor))
				.schedule();
	}

	//Temporal fix until superclass is fixed. Then just remove this class.
	private class DisableAllWidgetsJob extends org.jboss.tools.foundation.ui.jobs.DisableAllWidgetsJob {
		Composite container;

		public DisableAllWidgetsJob(boolean disable, Composite container, Cursor cursor) {
			super(disable, container, cursor);
		}

		public DisableAllWidgetsJob(boolean disableWidgets, Composite container, boolean disableCursor,
				Cursor cursor) {
			super(disableWidgets, container, disableCursor, cursor);
			this.container = container;
		}

		@Override
		public void run() {
			if(container != null && !container.isDisposed()) {
				super.run();
			}
		}
	}
	
}
