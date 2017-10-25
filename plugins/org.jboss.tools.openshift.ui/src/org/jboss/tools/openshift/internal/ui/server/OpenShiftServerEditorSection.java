/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat Inc..
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
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
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
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.editor.ServerEditor;
import org.eclipse.wst.server.ui.internal.editor.ServerEditorPartInput;
import org.eclipse.wst.server.ui.internal.editor.ServerResourceCommandManager;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
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
import org.jboss.tools.openshift.internal.common.ui.databinding.DisableableMultiValitdator;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormEditorPresenter;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.databinding.NumericValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.validator.OpenShiftIdentifierValidator;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 * @author Viacheslav Kabanovich
 * @author Rob Stryker
 */
public class OpenShiftServerEditorSection extends ServerEditorSection {

	private IServerEditorPartInput input;
	private OpenShiftServerEditorModel model;
	private DataBindingContext dbc;
	private FormPresenterSupport formPresenterSupport;

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

		this.dbc = new DataBindingContext();
		this.formPresenterSupport = new FormPresenterSupport(
				new FormEditorPresenter(getManagedForm().getForm().getForm()), dbc);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());

		this.model = new OpenShiftServerEditorModel(server, this, null);

		Section section = toolkit.createSection(parent,
				ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR);
		section.setText("OpenShift Server Adapter");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(section);

		Composite container = createControls(toolkit, section, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(container);
		toolkit.paintBordersFor(container);
		toolkit.adapt(container);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(container);
		
		section.setClient(container);

		setupWarningToRestartAdapter(getServerEditor());

		loadResources(section, model, dbc);
		dbc.updateTargets();
	}

	private Composite createControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		
		createConnectionContols(container, dbc);
		createEclipseProjectControls(toolkit, container, dbc);
		createOpenShiftApplicationControls(toolkit, container, dbc);
		createDebuggingControls(toolkit, container, dbc);

		return container;
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
						new Job("Refresh OpenShift resources") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									model.loadResources();
								} catch (OpenShiftException | NullPointerException e) {
									OpenShiftUIActivator.log(IStatus.ERROR, 
											NLS.bind("Could not load resources for connection to {0}", model.getConnection().getHost())
											, e);
								}
								return Status.OK_STATUS;
							}

						}.schedule();
					}
				});

		ControlDecorationSupport.create(connectionBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));

		Button newConnectionButton = new Button(parent, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).hint(120, SWT.DEFAULT)
			.applyTo(newConnectionButton);
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

	protected void createEclipseProjectControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(3)
			.applyTo(container);
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>Eclipse Project Source (From)</b>", true, false);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.applyTo(title);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));

		createProjectControls(container, dbc);
		createProjectPathControls(container, dbc);
	}

	@SuppressWarnings("unchecked")
	private void createProjectControls(Composite parent, DataBindingContext dbc) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(projectLabel);

		Combo projectsCombo = new Combo(parent, SWT.DEFAULT);
		StructuredViewer projectsViewer = new ComboViewer(projectsCombo);
		GridDataFactory.fillDefaults()
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

		IObservableValue<IProject> selectedProjectObservable = ViewerProperties.singleSelection().observe(projectsViewer);
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
				.align(SWT.FILL, SWT.CENTER).hint(120, SWT.DEFAULT)
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

	@SuppressWarnings("unchecked")
	private void createProjectPathControls(Composite parent, DataBindingContext dbc) {
		Label sourcePathLabel = new Label(parent, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(sourcePathLabel);

		Text sourcePathText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.FILL).grab(true, false)
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

		Button browseSourceButton = new Button(parent, SWT.PUSH);
		browseSourceButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER).hint(120, SWT.DEFAULT)
				.applyTo(browseSourceButton);
		browseSourceButton.addSelectionListener(onBrowseSource(browseSourceButton.getShell()));

		Button browseWorkspaceSourceButton = new Button(parent, SWT.PUSH | SWT.READ_ONLY);
		browseWorkspaceSourceButton.setText("Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(120, SWT.DEFAULT)
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

	private void createOpenShiftApplicationControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(3)
			.applyTo(container);
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>OpenShift Application Destination (To)</b>", true, false);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.applyTo(title);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));
		createResourceControls(container, dbc);
		createPodPathControls(container, dbc);
	}
	
	@SuppressWarnings("unchecked")
	private void createPodPathControls(Composite parent, DataBindingContext dbc) {
		Label deployPathLabel = new Label(parent, SWT.NONE);
		deployPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(deployPathLabel);

		Text deployPathText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.span(2, 1).grab(true, false)
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

	@SuppressWarnings("unchecked")
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
				.align(SWT.FILL, SWT.CENTER).hint(120, SWT.DEFAULT)
				.applyTo(selectResourceButton);
		selectResourceButton.addSelectionListener(onSelectResource(model, selectResourceButton.getShell()));
	}

	private void createDebuggingControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(5)
			.applyTo(container);
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>Debugging Settings</b>", true, false);
		GridDataFactory.fillDefaults()
			.span(5, 1)
			.applyTo(title);
		GridDataFactory.fillDefaults()
			.span(5, 1)
			.applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));
		createEnableDebuggingControls(toolkit, container, dbc);
		createDebuggingPortControls(toolkit, container, dbc);
	}
	
	@SuppressWarnings("unchecked")
	private void createEnableDebuggingControls(FormToolkit toolkit, Composite container, DataBindingContext dbc) {
		Label enableDevmodeLabel = new Label(container, SWT.None);
		enableDevmodeLabel.setText("Enable debugging:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(enableDevmodeLabel);
		Button useImageDevmodeKeyButton = toolkit.createButton(container, "use image provided key", SWT.CHECK);
		GridDataFactory.fillDefaults()
			.span(4, 1).align(SWT.FILL, SWT.CENTER)
			.applyTo(useImageDevmodeKeyButton);
		ISWTObservableValue useImageDevmodeObservable = WidgetProperties.selection().observe(useImageDevmodeKeyButton);
		ValueBindingBuilder
			.bind(useImageDevmodeObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEVMODE_KEY).observe(model))
			.in(dbc);
		// filler
		new Label(container, SWT.NONE);
		Label keyLabel = new Label(container, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(keyLabel);
		Text devmodeKeyText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.FILL, SWT.CENTER)
			.applyTo(devmodeKeyText);
		IObservableValue<String> devmodeKeyObservable = WidgetProperties.text(SWT.Modify).observe(devmodeKeyText);
		ValueBindingBuilder
			.bind(devmodeKeyObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEVMODE_KEY).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(devmodeKeyText))
			.notUpdating(useImageDevmodeObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		
		ValidationStatusProvider devmodeKeyValidator = 
				new DisableableMultiValitdator<String>(useImageDevmodeObservable, devmodeKeyObservable, 
						new OpenShiftIdentifierValidator());
		dbc.addValidationStatusProvider(devmodeKeyValidator);
		ControlDecorationSupport.create(devmodeKeyValidator, SWT.LEFT | SWT.TOP, container,
				new RequiredControlDecorationUpdater(true));
	}

	@SuppressWarnings("unchecked")
	private void createDebuggingPortControls(FormToolkit toolkit, Composite container, DataBindingContext dbc) {
		Label debugPortLabel = new Label(container, SWT.None);
		debugPortLabel.setText("Debugging Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(debugPortLabel);

		// use image key & value checkbox
		Button useImageDebugPortKeyButton = new Button(container, SWT.CHECK);
		useImageDebugPortKeyButton.setText("use image provided key and value");
		GridDataFactory.fillDefaults()
			.span(4, 1).align(SWT.FILL, SWT.CENTER)
			.applyTo(useImageDebugPortKeyButton);
		IObservableValue<Boolean> useImageDebugPortKey = 
				WidgetProperties.selection().observe(useImageDebugPortKeyButton);
		ValueBindingBuilder
			.bind(useImageDebugPortKey)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEBUG_PORT_KEY).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(useImageDebugPortKey)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEBUG_PORT_VALUE).observe(model))
			.in(dbc);

		// port key field
		new Label(container, SWT.NONE); // filler
		Label keyLabel = new Label(container, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(keyLabel);
		Text debugPortKeyText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(debugPortKeyText);
		IObservableValue<String> debugPortKeyObservable = 
				WidgetProperties.text(SWT.Modify).observe(debugPortKeyText);
		ValueBindingBuilder
			.bind(debugPortKeyObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEBUG_PORT_KEY).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(debugPortKeyText))
			.notUpdating(useImageDebugPortKey)
			.converting(new InvertingBooleanConverter())
			.in(dbc);

		ValidationStatusProvider debugPortKeyValidator = 
				new DisableableMultiValitdator<String>(useImageDebugPortKey, debugPortKeyObservable, 
						new OpenShiftIdentifierValidator());
		dbc.addValidationStatusProvider(debugPortKeyValidator);
		ControlDecorationSupport.create(debugPortKeyValidator, SWT.LEFT | SWT.TOP, container,
				new RequiredControlDecorationUpdater(true));
		
		// port value field
		Label portLabel = new Label(container, SWT.NONE);
		portLabel.setText("Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(portLabel);
		Text debugPortText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(debugPortText);
		IObservableValue<String> debugPortValueObservable = 
				WidgetProperties.text(SWT.Modify).observe(debugPortText);
		Binding debugPortBinding = ValueBindingBuilder
			.bind(debugPortValueObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEBUG_PORT_VALUE).observe(model))
			.in(dbc);
		ControlDecorationSupport.create(debugPortBinding,SWT.LEFT | SWT.TOP, container
				, new RequiredControlDecorationUpdater(true));
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(debugPortText))
			.notUpdating(useImageDebugPortKey)
			.converting(new InvertingBooleanConverter())
			.in(dbc);

		ValidationStatusProvider debugPortValueValidator = 
				new DisableableMultiValitdator<String>(useImageDebugPortKey, debugPortValueObservable, 
						new NumericValidator("integer", Integer::parseInt, true));
		dbc.addValidationStatusProvider(debugPortValueValidator);
		ControlDecorationSupport.create(debugPortValueValidator, SWT.LEFT | SWT.TOP, container,
				new RequiredControlDecorationUpdater(true));
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

	private class LoadResourcesJob extends Job {
		private OpenShiftServerEditorModel model;
		private IServerWorkingCopy server;
		private IProject deployProject;
		public LoadResourcesJob(OpenShiftServerEditorModel model, IServerWorkingCopy server, IProject deployProject) {
			super("Loading OpenShift Server Resources...");
			this.model = model;
			this.server = server;
			this.deployProject = deployProject;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			model.setInitializing(true);

			Connection con = OpenShiftServerUtils.getConnection(server);
			if (con != null) {
				model.loadResources(con);
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
			
			if (con != null) {
				model.setConnection(con);
				try {
					// Do service last, since it's most likely to error
					model.setResource(OpenShiftServerUtils.getResource(server, monitor));
				} finally {
					model.setInitializing(false);
				}
			}			
			return Status.OK_STATUS;
		}
	}
	
	private void loadResources(final Composite container, OpenShiftServerEditorModel model, DataBindingContext dbc) {
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
				new DisableAllWidgetsJobFixed(true, container, busyCursor, dbc), chainProgressMonitor)
			.runWhenDone(new LoadResourcesJob(model, server, deployProject))
			.runWhenDone(new DisableAllWidgetsJobFixed(false, container, false, busyCursor, dbc))
			.schedule();
	}

	private void setupWarningToRestartAdapter(IEditorPart editor) {
		if (editor == null) {
			return;
		}

		IObservableValue<Boolean> dirtyStatusObservable = DataBindingUtils.createDirtyStatusObservable(editor);
		dbc.addValidationStatusProvider(new MultiValidator() {

			@Override
			protected IStatus validate() {
				if (Boolean.TRUE.equals(dirtyStatusObservable.getValue())) {
					return ValidationStatus.warning(
							"Changes will only get active once the editor is saved and the adapter restarted.");
				}
				return ValidationStatus.ok();
			}
		});
	}

	private ServerEditor getServerEditor() {
		if (!(input instanceof ServerEditorPartInput)) {
			return null;
		}
		ServerResourceCommandManager commandManager = ((ServerEditorPartInput) input).getServerCommandManager();
		return commandManager.getServerEditor();
	}

	@Override
	public IStatus[] getSaveStatus() {
		return new IStatus[] { ignoreWarning(getFormPresenterStatus()) };
	}

	private IStatus getFormPresenterStatus() {
		IStatus status = Status.OK_STATUS;
		if (formPresenterSupport.getCurrentStatus() != null) {
			status = formPresenterSupport.getCurrentStatus();
		}
		return status;
	}
	
	/**
	 * Replaces WARNING status by an OK Status. This is useful when the given status
	 * is used to report the save state. warnings upon saving dont prevent saving
	 * but they're displayed in a dialog, something that you might want to prevent.
	 * 
	 * @param status
	 * @return
	 * 
	 * @see IStatus#WARNING
	 * @see IStatus#OK
	 */
	private IStatus ignoreWarning(IStatus status) {
		if (IStatus.WARNING == status.getSeverity()) {
			return Status.OK_STATUS;
		}
		return status;
	}

	@Override
	public void dispose() {
		formPresenterSupport.dispose();
		model.dispose();
		dbc.dispose();
	}

	//Temporal fix until superclass is fixed. Then just remove this class.
	private class DisableAllWidgetsJobFixed extends org.jboss.tools.foundation.ui.jobs.DisableAllWidgetsJob {
		Composite container;
		private DataBindingContext dbc;

		public DisableAllWidgetsJobFixed(boolean disable, Composite container, Cursor cursor, DataBindingContext dbc) {
			super(disable, container, cursor);
			this.dbc = dbc;
		}

		public DisableAllWidgetsJobFixed(boolean disableWidgets, Composite container, boolean disableCursor,
				Cursor cursor, DataBindingContext dbc) {
			super(disableWidgets, container, disableCursor, cursor);
			this.container = container;
			this.dbc = dbc;
		}

		@Override
		public void run() {
			if(container != null && !container.isDisposed()) {
				super.run();
				if (dbc != null ) {
					dbc.updateTargets();
				}
			}
		}
	}
}
