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

import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.DOWNLOAD_INSTRUCTIONS_URL;
import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.OPEN_SHIFT_PREFERENCE_PAGE_ID;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
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
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.jboss.ide.eclipse.as.ui.editor.DeploymentTypeUIUtil.ICompletable;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.OpenShiftCoreException;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.server.OpenShiftServerUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonUIMessages;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.SelectProjectComponentBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.DisableableMultiValitdator;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport;
import org.jboss.tools.openshift.internal.common.ui.databinding.FormPresenterSupport.IFormPresenter;
import org.jboss.tools.openshift.internal.common.ui.databinding.NumericValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DialogAdvancedPart;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.dialog.SelectRouteDialog.RouteLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.validator.OpenShiftIdentifierValidator;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard;

import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author Andre Dietisheim
 * @author Jeff Maury
 */
public class ServerSettingsWizardPage extends AbstractOpenShiftWizardPage implements ICompletable {
	private static final String DOWNLOAD_LINK_TEXT = "download";
	private static final int RESOURCE_PANEL_WIDTH = 800;
	private static final int RESOURCE_TREE_WIDTH = 400;
	private static final int RESOURCE_TREE_HEIGHT = 120;
	
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
	 * @param resource the selected resource
	 */
	protected ServerSettingsWizardPage(final IWizard wizard, final IServerWorkingCopy server, final Connection connection, 
			final IResource resource, final IRoute route) {
		this(wizard, server, connection, resource, route, null);
	}

	protected ServerSettingsWizardPage(final IWizard wizard, final IServerWorkingCopy server, final Connection connection, 
			final IResource resource, final IRoute route, final IProject deployProject) {
		super("Server Settings", 
				"Create an OpenShift 3 Server Adapter by selecting the project, resource and folders used for file synchronization.", 
				"Create an OpenShift 3 Server Adapter", 
				wizard);
		this.model = new ServerSettingsWizardPageModel(resource, route, deployProject, connection, server, 
				OCBinary.getInstance().getStatus(connection, new NullProgressMonitor()));
	}
	
	/**
	 * @return the {@link ServerSettingsWizardPageModel} associated with this page.
	 */
	ServerSettingsWizardPageModel getModel() {
		return model;
	}
	
	void updateServer() {
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
					    if (getContainer() != null) {
	                        ServerSettingsWizardPage.this.setMessage(message, type);
					    }
					}

					@Override
					public void setComplete(boolean complete) {
					    if (getContainer() != null) {
	                        ServerSettingsWizardPage.this.setComplete(complete);
					    }
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
	
	private Composite createControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.grab(true, true)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.margins(6, 6)
			.applyTo(container);

		createOCWarningControls(container, model, dbc);
		createEclipseProjectSourceControls(container, model, dbc);
		createOpenShiftDestinationControls(container, model, dbc);
		createAdvancedGroup(container, dbc);

		this.uiHook = container;
		return container;
	}

	@SuppressWarnings("unchecked")
	private void createOCWarningControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
        Composite composite = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults()
        		.applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
        
        ValueBindingBuilder
                .bind(WidgetProperties.visible().observe(composite))
                .to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_OC_BINARY_STATUS).observe(model))
                .converting(new Converter(IStatus.class, Boolean.class) {

                    @Override
                    public Object convert(Object fromObject) {
                        return !((IStatus)fromObject).isOK();
                    }
                    
                })
                .in(dbc);

        Label label = new Label(composite, SWT.NONE);
        ValueBindingBuilder
            .bind(WidgetProperties.image().observe(label))
            .to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_OC_BINARY_STATUS).observe(model))
            .converting(new Converter(IStatus.class, Image.class) {

                @Override
                public Object convert(Object fromObject) {
                	switch (((IStatus)fromObject).getSeverity()) {
                    	case IStatus.WARNING:
                            return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
                        case IStatus.ERROR:
                            return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR);
                        default:
                        	return null;
                    }
                }
             })
            .in(dbc);
   
        Link link = new Link(composite, SWT.WRAP);
		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(link))
			.to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_OC_BINARY_STATUS).observe(model))
			.converting(new Converter(IStatus.class, String.class) {
	            @Override
	            public Object convert(Object fromObject) {
	                return ((IStatus)fromObject).getMessage();
	            }
			})
			.in(dbc);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (DOWNLOAD_LINK_TEXT.equals(e.text)) {
					new BrowserUtility().checkedCreateExternalBrowser(DOWNLOAD_INSTRUCTIONS_URL,
							OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				} else {
					int rc = PreferencesUtil.createPreferenceDialogOn(getShell(), OPEN_SHIFT_PREFERENCE_PAGE_ID,
							new String[] { OPEN_SHIFT_PREFERENCE_PAGE_ID }, null).open();
					if (rc == Dialog.OK) {
						new Job("Checking oc binary") {

							@Override
							protected IStatus run(IProgressMonitor monitor) {
								return OCBinary.getInstance().getStatus(model.getConnection(), monitor);
							}
						}.schedule();
					}
				}
			}
		});
        GridDataFactory.fillDefaults()
        		.hint(600, SWT.DEFAULT)
        		.applyTo(link);
		MultiValidator validator = new MultiValidator() {

			@Override
			protected IStatus validate() {
				IObservableValue<IStatus> observable = BeanProperties
						.value(ServerSettingsWizardPageModel.PROPERTY_OC_BINARY_STATUS).observe(model);
				Status status = (Status) observable.getValue();
				switch (status.getSeverity()) {
				case IStatus.ERROR:
					return OpenShiftUIActivator.statusFactory().errorStatus(OpenShiftUIMessages.OCBinaryErrorMessage);
				case IStatus.WARNING:
					return OpenShiftUIActivator.statusFactory()
							.warningStatus(OpenShiftUIMessages.OCBinaryWarningMessage);
				}
				return status;
			}
		};
		dbc.addValidationStatusProvider(validator);
	}
	
	private void createEclipseProjectSourceControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group container = new Group(parent, SWT.NONE);
		container.setText("Eclipse Project Source (From)");
		GridDataFactory.fillDefaults()
			.grab(true, false)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(4).margins(10,10).spacing(4, 4)
			.applyTo(container);

		createProjectControls(container, model, dbc);
		createSourcePathControls(container, model, dbc);
	}
	
	private void createProjectControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		@SuppressWarnings("unchecked")
		IObservableValue<IProject> eclipseProjectObservable = 
				BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_DEPLOYPROJECT).observe(model);
		new SelectProjectComponentBuilder()
			.setTextLabel("Eclipse Project: ")
			.setEclipseProjectObservable(eclipseProjectObservable)
			.setSelectionListener(onBrowseProjects(model, parent.getShell()))
			.build(parent, dbc);
		Button importButton = new Button(parent, SWT.PUSH);
		importButton.setText(OpenShiftCommonUIMessages.ImportButtonLabel);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER)
			.applyTo(importButton);
		UIUtils.setDefaultButtonWidth(importButton);
		importButton.addSelectionListener(onImportProject(model, parent.getShell()));
	}

	private IDoubleClickListener onDoubleClickService() {
		return event -> {
			if (getWizard().canFinish()) {
				Button finishButton = getShell().getDefaultButton();
				UIUtils.clickButton(finishButton);
			}
		};
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

    /**
     * Open a dialog box to import an Eclipse project when clicking on the 'Import' button.
     * 
     * @return
     */
    private SelectionListener onImportProject(ServerSettingsWizardPageModel model, final Shell shell) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	if (model.getResource() == null) {
            		MessageDialog.openWarning(shell, "No Build Configurations found", "A build config is used to import a project to Eclipse");
            		return;
            	}
                Map<com.openshift.restclient.model.IProject, Collection<IBuildConfig>> projectsAndBuildConfigs = new HashMap<>();
                projectsAndBuildConfigs.put(model.getResource().getProject(), Collections.emptyList());
                ImportApplicationWizard wizard = new ImportApplicationWizard(projectsAndBuildConfigs);
                final boolean done = WizardUtils.openWizardDialog(wizard, shell);
                if (done) {
                    model.setDeployProject(ResourcesPlugin.getWorkspace().getRoot().getProject(wizard.getModel().getRepoName()));
                }
            }
        };
    }

	@SuppressWarnings("unchecked")
    private void createSourcePathControls(Composite parent, ServerSettingsWizardPageModel model,
			DataBindingContext dbc) {    		
		Label sourcePathLabel = new Label(parent, SWT.NONE);
		sourcePathLabel.setText("Source Path: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(sourcePathLabel);

		Text sourcePathText = new Text(parent, SWT.BORDER);
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
		
		Button browseSourceButton = new Button(parent, SWT.PUSH);
		browseSourceButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.indent(10, SWT.DEFAULT)
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
	
	private void createOpenShiftDestinationControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group container = new Group(parent, SWT.NONE);
		container.setText("OpenShift Application Destination (To)");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10,10)
			.applyTo(container);

		createResourceControls(container, model, dbc);
		createResourcePathControls(container, model, dbc);
	}

	private void createResourceControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		SashForm resourceControlsContainer = new SashForm(parent, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).hint(RESOURCE_PANEL_WIDTH, SWT.DEFAULT)
			.applyTo(resourceControlsContainer);

		IViewerObservableValue selectedResourceTreeItem = 
				createResourceTree(model, resourceControlsContainer, dbc);
		createResourceDetails(selectedResourceTreeItem, resourceControlsContainer, dbc);

		resourceControlsContainer.setWeights(new int[] {1,2});
	}

	private void createResourceDetails(IViewerObservableValue selectedResourceTreeItem, Composite parent, DataBindingContext dbc) {
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		ExpandableComposite expandable = new ExpandableComposite(scrolledComposite, SWT.None);
		scrolledComposite.setContent(expandable);
		expandable.setText("Resource Details");
		expandable.setExpanded(true);
		expandable.setLayout(new FillLayout());
		Composite detailsContainer = new Composite(expandable, SWT.NONE);
		expandable.setClient(detailsContainer);
		expandable.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				getControl().update();
				((Composite) getControl()).layout(true);
			}
		});

		IObservableValue<IResource> selectedResource = new WritableValue<>();
		ValueBindingBuilder
			.bind(selectedResourceTreeItem)
			.converting(new ObservableTreeItem2ModelConverter())
			.to(selectedResource)
			.notUpdatingParticipant()
			.in(dbc);
		new ResourceDetailViews(selectedResource, detailsContainer, dbc).createControls();
	}

	@SuppressWarnings("unchecked")
	private IViewerObservableValue createResourceTree(ServerSettingsWizardPageModel model, 
			SashForm resourceControlsContainer, DataBindingContext dbc) {
		Composite resourceTreeContainer = new Composite(resourceControlsContainer, SWT.None);
		GridLayoutFactory.fillDefaults()
			.applyTo(resourceTreeContainer);
		
		// filter
		Text selectorText = UIUtils.createSearchText(resourceTreeContainer);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(selectorText);

		// resource tree
		final TreeViewer resourcesViewer = createResourcesTreeViewer(resourceTreeContainer, model, selectorText);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).hint(RESOURCE_TREE_WIDTH, RESOURCE_TREE_HEIGHT)
			.applyTo(resourcesViewer.getControl());
		resourcesViewer.addDoubleClickListener(onDoubleClickService());
		IObservableList<ObservableTreeItem> resourceItemsObservable = 
				BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_RESOURCE_ITEMS).observe(model);
		DataBindingUtils.addDisposableListChangeListener(
				onResourceItemsChanged(resourcesViewer), resourceItemsObservable, resourcesViewer.getTree());
		selectorText.addModifyListener(onFilterTextModified(resourcesViewer));
		IViewerObservableValue selectedResourceTreeItem = ViewerProperties.singleSelection().observe(resourcesViewer);
		ValueBindingBuilder
				.bind(selectedResourceTreeItem)
				.converting(new ObservableTreeItem2ModelConverter(IResource.class))
				.validatingAfterConvert(value -> {
					if ((value instanceof IResource)
							&& OpenShiftServerUtils.isAllowedForServerAdapter((IResource) value)) {
						return ValidationStatus.ok();
					}
					return ValidationStatus.cancel("Please select a resource that this adapter will be bound to.");
				})
				.to(BeanProperties.value(ServerSettingsWizardPageModel.PROPERTY_RESOURCE).observe(model))
				.converting(new Model2ObservableTreeItemConverter(new ServerSettingsWizardPageModel.ResourceTreeItemsFactory()))
				.in(dbc);
		return selectedResourceTreeItem;
	}

	@SuppressWarnings("unchecked")
	private void createResourcePathControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, true)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(0, 0)
			.applyTo(container);
		
		Button useInferredPodPathButton = new Button(container, SWT.CHECK);
		useInferredPodPathButton.setText("&Use inferred Pod Deployment Path");
		GridDataFactory.fillDefaults()
			.span(2,1).align(SWT.FILL, SWT.CENTER)
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
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
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

	private void createAdvancedGroup(Composite parent, DataBindingContext dbc) {
		DialogAdvancedPart advancedPart = new DialogAdvancedPart() {
			
			@Override
			protected void createAdvancedContent(Composite advancedComposite) {
				createDebuggingSettingsControls(advancedComposite, model, dbc);
				createRouteControls(advancedComposite, model, dbc);
			}

			@Override
			protected GridLayoutFactory adjustAdvancedCompositeLayout(GridLayoutFactory gridLayoutFactory) {
				return gridLayoutFactory.numColumns(1).margins(0, 0);
			}
		};
		advancedPart.createAdvancedGroup(parent, 1);
	}

	private void createDebuggingSettingsControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group container = new Group(parent, SWT.NONE);
		container.setText("Debugging Settings");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(container);
		GridLayoutFactory.fillDefaults()
			.numColumns(5).margins(10, 10)
			.applyTo(container);

		createEnableDebuggingControls(container, model, dbc);
		createDebuggingPortControls(container, model, dbc);
	}
	
	@SuppressWarnings("unchecked")
	private void createEnableDebuggingControls(Composite parent, ServerSettingsWizardPageModel model,
			DataBindingContext dbc) {
		Label enableDevmodeLabel = new Label(parent, SWT.None);
		enableDevmodeLabel.setText("Enable debugging:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(enableDevmodeLabel);
		Button useImageDevmodeKey = new Button(parent, SWT.CHECK);
		useImageDevmodeKey.setText("use image provided key");
		GridDataFactory.fillDefaults()
			.span(4, 1).align(SWT.FILL, SWT.CENTER)
			.applyTo(useImageDevmodeKey);
		IObservableValue<Boolean> useImageDevmodeKeyObservable = 
				BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEVMODE_KEY).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.selection().observe(useImageDevmodeKey))
			.to(useImageDevmodeKeyObservable)
			.in(dbc);
		// filler
		new Label(parent, SWT.NONE);
		Label keyLabel = new Label(parent, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(keyLabel);
		Text devmodeKeyText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(devmodeKeyText);
		IObservableValue<String> devmodeKeyObservable = WidgetProperties.text(SWT.Modify).observe(devmodeKeyText);
		ValueBindingBuilder
			.bind(devmodeKeyObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEVMODE_KEY).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(devmodeKeyText))
			.notUpdating(useImageDevmodeKeyObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValidationStatusProvider devmodeKeyValidator = 
				new DisableableMultiValitdator<String>(useImageDevmodeKeyObservable, devmodeKeyObservable,
						new OpenShiftIdentifierValidator());
		dbc.addValidationStatusProvider(devmodeKeyValidator);
		ControlDecorationSupport.create(devmodeKeyValidator, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));
	}

	@SuppressWarnings("unchecked")
	private void createDebuggingPortControls(Composite parent, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Label debugPortLabel = new Label(parent, SWT.None);
		debugPortLabel.setText("Debugging Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(debugPortLabel);

		// use image key & value checkbox
		Button useImageDebugPortKeyButton = new Button(parent, SWT.CHECK);
		useImageDebugPortKeyButton.setText("use image provided key and value");
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.FILL, SWT.CENTER)
			.applyTo(useImageDebugPortKeyButton);
		IObservableValue<Boolean> useImageDebugPortKey = 
				BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEBUG_PORT_KEY).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.selection().observe(useImageDebugPortKeyButton))
			.to(useImageDebugPortKey)
			.in(dbc);
		//filler
		new Label(parent, SWT.NONE);

		// key text field
		new Label(parent, SWT.NONE); // filler
		Label keyLabel = new Label(parent, SWT.NONE);
		keyLabel.setText("Key:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(keyLabel);
		Text debugPortKeyText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(debugPortKeyText);
		IObservableValue<String> debugPortKeyTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(debugPortKeyText);
		ValueBindingBuilder
			.bind(debugPortKeyTextObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEBUG_PORT_KEY).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(debugPortKeyText))
			.notUpdating(useImageDebugPortKey)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		
		ValidationStatusProvider debugPortKeyValidator = 
				new DisableableMultiValitdator<String>(useImageDebugPortKey, debugPortKeyTextObservable, 
						new OpenShiftIdentifierValidator());
		dbc.addValidationStatusProvider(debugPortKeyValidator);
		ControlDecorationSupport.create(debugPortKeyValidator, SWT.LEFT | SWT.TOP, parent,
				new RequiredControlDecorationUpdater(true));

		// port text field
		IObservableValue<Boolean> useImageDebugPortValue = 
				BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_USE_IMAGE_DEBUG_PORT_VALUE).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.selection().observe(useImageDebugPortKeyButton))
			.to(useImageDebugPortValue)
			.in(dbc);
		Label portLabel = new Label(parent, SWT.NONE);
		portLabel.setText("Port:");
		GridDataFactory.fillDefaults()
			.align(SWT.BEGINNING, SWT.CENTER)
			.applyTo(portLabel);
		Text debugPortText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(debugPortText);
		IObservableValue<String> debugPortValueObservable = 
				WidgetProperties.text(SWT.Modify).observe(debugPortText);
		ValueBindingBuilder
			.bind(debugPortValueObservable)
			.to(BeanProperties.value(OpenShiftServerEditorModel.PROPERTY_DEBUG_PORT_VALUE).observe(model))
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(debugPortText))
			.notUpdating(useImageDebugPortValue)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValidationStatusProvider debugPortValueValidator = 
				new DisableableMultiValitdator<String>(useImageDebugPortValue, debugPortValueObservable, 
						new NumericValidator("integer", Integer::parseInt, true));
		dbc.addValidationStatusProvider(debugPortValueValidator);
		ControlDecorationSupport.create(debugPortValueValidator, SWT.LEFT | SWT.TOP, parent,
				new RequiredControlDecorationUpdater(true));
	}
	
	@SuppressWarnings("unchecked")
	private void createRouteControls(Composite container, ServerSettingsWizardPageModel model, DataBindingContext dbc) {
		Group routeGroup = new Group(container, SWT.NONE);
		routeGroup.setText("Route");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
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

	private IListChangeListener<ObservableTreeItem> onResourceItemsChanged(final TreeViewer resourcesViewer) {
		return event ->	resourcesViewer.expandAll();
	}

	@SuppressWarnings("unchecked")
	private TreeViewer createResourcesTreeViewer(Composite parent, ServerSettingsWizardPageModel model, Text selectorText) {
		TreeViewer applicationTemplatesViewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty<ServerSettingsWizardPageModel, ObservableTreeItem> childrenProperty = new MultiListProperty<>(
				new IListProperty[] {
						BeanProperties.list(ServerSettingsWizardPageModel.PROPERTY_RESOURCE_ITEMS),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		applicationTemplatesViewer.setContentProvider(contentProvider);
		applicationTemplatesViewer.setLabelProvider(new ResourcesViewLabelProvider());
		applicationTemplatesViewer.addFilter(new ServiceViewerFilter(selectorText));
		applicationTemplatesViewer.setComparator(ProjectViewerComparator.createProjectTreeSorter());
		applicationTemplatesViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		applicationTemplatesViewer.setInput(model);
		return applicationTemplatesViewer;
	}	

	protected ModifyListener onFilterTextModified(final TreeViewer applicationTemplatesViewer) {
		return event -> {
			applicationTemplatesViewer.refresh();
			applicationTemplatesViewer.expandAll();
		};
	}

	@Override
	public void dispose() {
		super.dispose();
		uiHook = null;
		model.dispose();
	}

    @Override
	public boolean isPageComplete() {
        return !isLoadingResources 
        		&& uiHook != null && !uiHook.isDisposed() 
    			&& !needsLoadingResources 
    			&& model != null && model.getResource() != null 
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
			if (BooleanUtils.isFalse((Boolean) useDefaultRoute.getValue()) 
					&& selectedRoute.getValue() == null) {
				return ValidationStatus.cancel("You have to choose a route that will be used for this server adapter.");
			}
			return ValidationStatus.ok();
		}
		
	}
}
