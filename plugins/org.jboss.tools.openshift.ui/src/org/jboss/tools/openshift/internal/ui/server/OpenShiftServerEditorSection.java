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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.editor.IServerEditorPartInput;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.command.ServerCommand;
import org.eclipse.wst.server.ui.internal.editor.ServerEditor;
import org.eclipse.wst.server.ui.internal.editor.ServerEditorPartInput;
import org.eclipse.wst.server.ui.internal.editor.ServerResourceCommandManager;
import org.jboss.ide.eclipse.as.wtp.ui.editor.ServerWorkingCopyPropertyTextCommand;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormEditorPresenter;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.databinding.NumericValidator;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.ui.validator.OpenShiftIdentifierValidator;

import com.openshift.restclient.model.IResource;

/**
 * @author Andre Dietisheim
 * @author Viacheslav Kabanovich
 * @author Rob Stryker
 */
public class OpenShiftServerEditorSection extends ServerEditorSection {

	private IServerEditorPartInput input;
	private DataBindingContext dbc;
	private FormPresenterSupport formPresenterSupport;
	private InitialModel initialModel;
	private Text currentConnectionText;
	private Text projectText;
	private Text sourcePathText;
	private Label resourceKindLabel;
	private Text resourceText;
	private Text deployPathText;
	private Text devmodeKeyText;
	private Text debugPortText, debugPortKeyText;
	private Button useImageDebugPortKeyButton;
	private ModifyListener deployPathModifyListener;
	private ModifyListener devmodeKeyModifyListener;
	private ModifyListener debugPortKeyListener, debugPortValListener;
	private SelectionListener debugPortKeyValSelectionListener;
	private SelectionAdapter useImageDevmodeKeyListener;
	private Button useImageDevmodeKeyButton;
	
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
		Section section = toolkit.createSection(parent,
				ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR);
		section.setText("OpenShift Server Adapter");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(section);

		Composite container = createControls(toolkit, section, dbc);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		toolkit.paintBordersFor(container);
		toolkit.adapt(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		section.setClient(container);

		setupWarningToRestartAdapter(getServerEditor());

		loadResources(section, dbc);
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
		currentConnectionText = new Text(comboHolder, SWT.SINGLE | SWT.BORDER);
		currentConnectionText.setEnabled(false);
		currentConnectionText.setEditable(false);
	}


	protected void createEclipseProjectControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>Eclipse Project Source (From)</b>", true, false);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(title);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));

		createProjectControls(container, dbc);
		createProjectPathControls(container, dbc);
	}

	@SuppressWarnings("unchecked")
	private void createProjectControls(Composite parent, DataBindingContext dbc) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText("Eclipse Project: ");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(projectLabel);

		projectText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(projectText);
	}

	@SuppressWarnings("unchecked")
	private void createProjectPathControls(Composite parent, DataBindingContext dbc) {
		Label sourcePathLabel = new Label(parent, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(sourcePathLabel);

		sourcePathText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(sourcePathText);
		new Label(parent, SWT.NONE);
	}

	private void createOpenShiftApplicationControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>OpenShift Application Destination (To)</b>", true, false);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(title);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));
		createResourceControls(container, dbc);
		createPodPathControls(container, dbc);
	}
	
	@SuppressWarnings("unchecked")
	private void createPodPathControls(Composite parent, DataBindingContext dbc) {
		Label deployPathLabel = new Label(parent, SWT.NONE);
		deployPathLabel.setText("Pod Deployment Path: ");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(deployPathLabel);

		deployPathText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(deployPathText);
		
		deployPathModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new ServerWorkingCopyPropertyTextCommand(input.getServer(), "Set Deploy Path", deployPathText, 
						deployPathText.getText(), OpenShiftServerUtils.ATTR_POD_PATH, (ModifyListener) this));
			}
		};
		
		ModifyListener deployPathDecoration = new TextboxDecoratorSupport(deployPathText, 
				value -> {
					// TODO validate 
					if (!(value instanceof String) || StringUtils.isEmpty(value)) {
						return ValidationStatus.cancel("Please provide a path to deploy to on the pod.");
					}
					if (!Path.isValidPosixPath((String)value)) {
						return ValidationStatus.error("Please provide a valid path to deploy to on the pod");
					}
					return Status.OK_STATUS;
			});

		deployPathText.addModifyListener(deployPathDecoration);
	}

	@SuppressWarnings("unchecked")
	private void createResourceControls(Composite parent, DataBindingContext dbc) {
		resourceKindLabel = new Label(parent, SWT.None);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(resourceKindLabel);
		resourceText = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(resourceText);
	}

	private void createDebuggingControls(FormToolkit toolkit, Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(3,1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
		
		FormText title = toolkit.createFormText(container, false);
		title.setText("<b>Debugging Settings</b>", true, false);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(title);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(toolkit.createSeparator(container, SWT.HORIZONTAL));
		createEnableDebuggingControls(toolkit, container, dbc);
		createDebuggingPortControls(toolkit, container, dbc);
	}
	
	
	@SuppressWarnings("unchecked")
	private void createEnableDebuggingControls(FormToolkit toolkit, Composite container, DataBindingContext dbc) {
		Label enableDevmodeLabel = new Label(container, SWT.None);
		enableDevmodeLabel.setText("Enable debugging:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(enableDevmodeLabel);
		useImageDevmodeKeyButton = toolkit.createButton(container, "use image provided key", SWT.CHECK);
		GridDataFactory.fillDefaults().span(4, 1).align(SWT.FILL, SWT.CENTER).applyTo(useImageDevmodeKeyButton);
		// filler
		new Label(container, SWT.NONE);
		Label keyLabel = new Label(container, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(keyLabel);
		devmodeKeyText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).applyTo(devmodeKeyText);
		
		devmodeKeyModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new ServerWorkingCopyPropertyTextCommand(input.getServer(), "SetDevmodeKey", devmodeKeyText, 
						devmodeKeyText.getText(), OpenShiftServerUtils.ATTR_DEVMODE_KEY, (ModifyListener) this));
			}
		};
		
		ModifyListener devmodeKeyModifyDecoration = new TextboxDecoratorSupport(devmodeKeyText, 
				value -> {
					if( !useImageDevmodeKeyButton.getSelection()) {
						return new OpenShiftIdentifierValidator().validate(devmodeKeyText.getText());
					}
					return Status.OK_STATUS;
		});
		devmodeKeyText.addModifyListener(devmodeKeyModifyDecoration);
		
		useImageDevmodeKeyListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new ToggleDebuggingCommand(input.getServer(), useImageDevmodeKeyButton, devmodeKeyText, this, devmodeKeyModifyListener));
			}
		};
	}
	
	private static class ToggleDebuggingCommand extends ServerCommand {
		private IServerWorkingCopy wc;
		private Button useImageDevmodeKeyButton;
		private Text devmodeKeyText;
		private SelectionListener toggleListener;
		private ModifyListener textListener;
		private boolean preSelected;
		private String preText;
		
		public ToggleDebuggingCommand(IServerWorkingCopy server, Button useImageDevmodeKeyButton, Text devmodeKeyText,
				SelectionListener toggleListener, ModifyListener textListener) {
			super(server, "Enable Debugging");
			this.wc = server;
			this.useImageDevmodeKeyButton = useImageDevmodeKeyButton;
			this.devmodeKeyText = devmodeKeyText;
			this.preSelected = !useImageDevmodeKeyButton.getSelection();
			this.preText = devmodeKeyText.getText();
			this.toggleListener = toggleListener;
			this.textListener = textListener;
		}

		@Override
		public void execute() {
			useImageDevmodeKeyButton.removeSelectionListener(toggleListener);
			devmodeKeyText.removeModifyListener(textListener);
			
			if( useImageDevmodeKeyButton.getSelection() ) 
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEVMODE_KEY, (String)null);
			else
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEVMODE_KEY, "");
			
			devmodeKeyText.setEnabled(preSelected);
			devmodeKeyText.setText("");
			
			useImageDevmodeKeyButton.addSelectionListener(toggleListener);
			devmodeKeyText.addModifyListener(textListener);
		}

		@Override
		public void undo() {
			useImageDevmodeKeyButton.removeSelectionListener(toggleListener);
			devmodeKeyText.removeModifyListener(textListener);
			wc.setAttribute(OpenShiftServerUtils.ATTR_DEVMODE_KEY, preText);
			devmodeKeyText.setEnabled(!preSelected);
			useImageDevmodeKeyButton.setSelection(preSelected);
			devmodeKeyText.setText(preText);
			useImageDevmodeKeyButton.addSelectionListener(toggleListener);
			devmodeKeyText.addModifyListener(textListener);
		}
		
	}

	@SuppressWarnings("unchecked")
	private void createDebuggingPortControls(FormToolkit toolkit, Composite container, DataBindingContext dbc) {
		Label debugPortLabel = new Label(container, SWT.None);
		debugPortLabel.setText("Debugging Port:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(debugPortLabel);

		// use image key & value checkbox
		useImageDebugPortKeyButton = new Button(container, SWT.CHECK);
		useImageDebugPortKeyButton.setText("use image provided key and value");
		GridDataFactory.fillDefaults().span(4, 1).align(SWT.FILL, SWT.CENTER).applyTo(useImageDebugPortKeyButton);

		// port key field
		new Label(container, SWT.NONE); // filler
		Label keyLabel = new Label(container, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(keyLabel);
		debugPortKeyText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(debugPortKeyText);
		debugPortKeyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				execute(new ServerWorkingCopyPropertyTextCommand(input.getServer(), "SetDebugPortKey", debugPortKeyText, 
						debugPortKeyText.getText(), OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, (ModifyListener) this));
			}
		};

		ModifyListener debugPortKeyDecoration = new TextboxDecoratorSupport(debugPortKeyText, 
				value -> {
					if( !useImageDebugPortKeyButton.getSelection()) {
						return new OpenShiftIdentifierValidator().validate(debugPortKeyText.getText());
					}
					return Status.OK_STATUS;
		});
		debugPortKeyText.addModifyListener(debugPortKeyDecoration);

		
		// port value field
		Label portLabel = new Label(container, SWT.NONE);
		portLabel.setText("Port:");
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(portLabel);
		debugPortText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(debugPortText);
		debugPortValListener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				execute(new ServerWorkingCopyPropertyTextCommand(input.getServer(), "SetDebugPortValue", debugPortText, 
						debugPortText.getText(), OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, this));
			}
		}; 
		
		
		ModifyListener debugPortValDecoration = new TextboxDecoratorSupport(debugPortText, 
				value -> {
					if( !useImageDebugPortKeyButton.getSelection()) {
						return new NumericValidator("integer", Integer::parseInt, true).validate(debugPortText.getText());
					}
					return Status.OK_STATUS;
		});
		debugPortText.addModifyListener(debugPortValDecoration);

		
		debugPortKeyValSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				execute(new ToggleDebugKeyValueCommand(server, useImageDebugPortKeyButton, 
						this, debugPortKeyText, debugPortKeyListener, debugPortText, debugPortValListener));
			}
		};
	}

	private class TextboxDecoratorSupport implements ModifyListener {
		Text t;
		IValidator v;
		ControlDecoration decorator;

		public TextboxDecoratorSupport(Text text, IValidator validator) {
			this.t = text;
			this.v = validator;
			decorator = new ControlDecoration(t, SWT.CENTER);
		}

		@Override
		public void modifyText(ModifyEvent e) {
			IStatus s = v.validate(t.getText());
			if (!s.isOK()) {
				String imgKey = null;
				if (s.getSeverity() == IStatus.ERROR) {
					imgKey = FieldDecorationRegistry.DEC_ERROR;
				} else if (s.getSeverity() == IStatus.WARNING) {
					imgKey = FieldDecorationRegistry.DEC_WARNING;
				}
				if (imgKey != null) {
					Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(imgKey).getImage();
					decorator.setImage(image);
					decorator.setDescriptionText(s.getMessage());
					decorator.show();
				} else {
					decorator.hide();
				}
			} else {
				decorator.hide();
			}
		}

	}

	private static class ToggleDebugKeyValueCommand extends ServerCommand {
		private IServerWorkingCopy wc;
		private Button useImageDebugPortKeyButton;
		private SelectionListener toggleListener;
		
		private Text debugPortKeyText, debugPortText;
		private ModifyListener keyModifier, valModifier;
		
		private boolean preSelected;
		private String preKey, preVal;
		
		public ToggleDebugKeyValueCommand(IServerWorkingCopy server, Button useImageDebugPortKeyButton, SelectionListener toggleListener,  
				Text debugPortKeyText, ModifyListener keyModifier,
				Text debugPortText, ModifyListener valModifier) {
			super(server, "Enable Debugging");
			this.wc = server;
			this.useImageDebugPortKeyButton = useImageDebugPortKeyButton;
			this.toggleListener = toggleListener;
			
			this.debugPortKeyText = debugPortKeyText;
			this.debugPortText = debugPortText;
			this.keyModifier = keyModifier;
			this.valModifier = valModifier;
			
			this.preSelected = !useImageDebugPortKeyButton.getSelection();
			this.preKey = debugPortKeyText.getText();
			this.preVal = debugPortText.getText(); 
		}

		@Override
		public void execute() {
			useImageDebugPortKeyButton.removeSelectionListener(toggleListener);
			debugPortKeyText.removeModifyListener(keyModifier);
			debugPortText.removeModifyListener(valModifier);
			
			if( useImageDebugPortKeyButton.getSelection() ) {
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, (String)null);
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, (String)null);
			} else {
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, "");
				wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, "");
			}
			
			debugPortKeyText.setEnabled(preSelected);
			debugPortText.setEnabled(preSelected);
			debugPortKeyText.setText("");
			debugPortText.setText("");
			
			useImageDebugPortKeyButton.addSelectionListener(toggleListener);
			debugPortKeyText.addModifyListener(keyModifier);
			debugPortText.addModifyListener(valModifier);
		}

		@Override
		public void undo() {
			useImageDebugPortKeyButton.removeSelectionListener(toggleListener);
			debugPortKeyText.removeModifyListener(keyModifier);
			debugPortText.removeModifyListener(valModifier);
			
			wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, preKey);
			wc.setAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, preVal);

			debugPortKeyText.setEnabled(!preSelected);
			debugPortText.setEnabled(!preSelected);
			useImageDebugPortKeyButton.setSelection(preSelected);
			debugPortKeyText.setText(preKey);
			debugPortText.setText(preVal);
			
			useImageDebugPortKeyButton.addSelectionListener(toggleListener);
			debugPortKeyText.addModifyListener(keyModifier);
			debugPortText.addModifyListener(valModifier);
		}
		
	}
	
	private class InitialModel {
		private Connection con;
		private IProject deployProj;
		private String sourcePath;
		private String podPath;
		private IResource openshiftResource;

		public InitialModel(Connection con, IProject deployProj, String sourcePath, String podPath, IResource openshiftResource) {
			this.con = con;
			this.deployProj = deployProj;
			this.sourcePath = sourcePath;
			this.podPath = podPath;
			this.openshiftResource = openshiftResource;
		}
	}
	
	private class LoadResourcesJob extends Job {
		private IServerWorkingCopy server;
		public LoadResourcesJob(IServerWorkingCopy server) {
			super("Loading OpenShift Server Resources...");
			this.server = server;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final IProject deployProject = OpenShiftServerUtils.getDeployProject(server);
			Connection con = OpenShiftServerUtils.getConnection(server);
			String sourcePath = OpenShiftServerUtils.getSourcePath(server);
			String podPath = OpenShiftServerUtils.getPodPath(server);
			IResource resource = null;
			if (con != null) {
				resource = OpenShiftServerUtils.getResource(server, monitor);
			}
			InitialModel im = new InitialModel(con, deployProject, sourcePath, podPath, resource);
			setInitialModel(im);
			return Status.OK_STATUS;
		}
	}
	
	
	private void setInitialModel(final InitialModel im) {
		this.initialModel = im;
	}
	
	private void loadResources(final Composite container, DataBindingContext dbc) {
		IServerWorkingCopy server = input.getServer();
		Cursor busyCursor = new Cursor(container.getDisplay(), SWT.CURSOR_WAIT);
		IProgressMonitor chainProgressMonitor = new NullProgressMonitor() {
			@Override
			public boolean isCanceled() {
				return container.isDisposed();
			}
		};
		new JobChainBuilder(
				new DisableAllWidgetsJobFixed(true, container, busyCursor, dbc), chainProgressMonitor)
			.runWhenDone(new LoadResourcesJob(server))
			.runWhenDone(new DisableAllWidgetsJobFixed(false, container, false, busyCursor, dbc))
			.runWhenDone(new InitializeWidgetsJob())
			.schedule();
	}

	private class InitializeWidgetsJob extends UIJob {
		
		public InitializeWidgetsJob() {
			super(getServerEditor().getEditorSite().getShell().getDisplay(), "Initialize Controls");
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			initializeWidgetValues();
			addListeners();
			return Status.OK_STATUS;
		}

		private void addListeners() {
			// Add listeners now
			useImageDebugPortKeyButton.addSelectionListener(debugPortKeyValSelectionListener);
			deployPathText.addModifyListener(deployPathModifyListener);
			devmodeKeyText.addModifyListener(devmodeKeyModifyListener);
			useImageDevmodeKeyButton.addSelectionListener(useImageDevmodeKeyListener);

			debugPortText.addModifyListener(debugPortValListener);
			debugPortKeyText.addModifyListener(debugPortKeyListener);
		}

		private String nullSafe(String s) {
			return s == null ? "" : s;
		}
		private void initializeWidgetValues() {
			if( initialModel.con != null )
				currentConnectionText.setText(nullSafe(initialModel.con.toString()));
			
			if( initialModel.deployProj != null )
				projectText.setText(nullSafe(initialModel.deployProj.getName()));
			
			if( initialModel.sourcePath != null ) 
				sourcePathText.setText(initialModel.sourcePath);
			
			if( initialModel.openshiftResource != null ) {
				String kind = nullSafe(initialModel.openshiftResource.getKind()) + ":";
				resourceKindLabel.setText(kind);
				String namespaceAndName = nullSafe(initialModel.openshiftResource.getNamespace()) + "/" + 
										  nullSafe(initialModel.openshiftResource.getName());
				resourceText.setText(namespaceAndName);
			} else {
				resourceKindLabel.setText("Resource: ");
				resourceText.setText("<not found>");
			}
			
			if( initialModel.podPath != null )
				deployPathText.setText(initialModel.podPath);

			String currentKey = input.getServer().getAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_KEY, (String)null);
			String currentVal = input.getServer().getAttribute(OpenShiftServerUtils.ATTR_DEBUG_PORT_VALUE, (String)null);
			if( StringUtils.isEmpty(currentKey) && StringUtils.isEmpty(currentVal)) {
				useImageDebugPortKeyButton.setSelection(true);
				debugPortKeyText.setText("");
				debugPortText.setText("");
				debugPortKeyText.setEnabled(false);
				debugPortText.setEnabled(false);
			} else {
				useImageDebugPortKeyButton.setSelection(false);
				debugPortKeyText.setText(currentKey);
				debugPortText.setText(currentVal);
				debugPortKeyText.setEnabled(true);
				debugPortText.setEnabled(true);
			}

			String devModeCurrentVal = input.getServer().getAttribute(OpenShiftServerUtils.ATTR_DEVMODE_KEY, (String) null);
			if (StringUtils.isEmpty(devModeCurrentVal)) {
				useImageDevmodeKeyButton.setSelection(true);
			} else {
				useImageDevmodeKeyButton.setSelection(false);
				devmodeKeyText.setText(devModeCurrentVal);
			}
			devmodeKeyText.setEnabled(!useImageDevmodeKeyButton.getSelection());
		}
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
		IStatus stat = formPresenterSupport.getCurrentStatus();
		if (stat != null) {
			return stat;
		}
		return Status.OK_STATUS;
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
