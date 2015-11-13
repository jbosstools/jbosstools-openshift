/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.TabFolderSelectionProperty;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.builder.ProjectBuilderTypeDetector;
import org.jboss.tools.openshift.internal.ui.wizard.project.ManageProjectsWizard;
import org.jboss.tools.openshift.internal.ui.wizard.project.NewProjectWizard;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.UnsupportedVersionException;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list templates to a user
 *
 * @author jeff.cantrill
 * @author Andre Dietisheim
 *
 */
public class TemplateListPage  extends AbstractOpenShiftWizardPage  {

	private static final int LOCAL_TEMPLATE_TAB_INDEX = 1;

	private ITemplateListPageModel model;
	private TreeViewer templatesViewer;

	public TemplateListPage(IWizard wizard, ITemplateListPageModel model) {
		super("Select template", 
				"Server template choices may be filtered by typing the name of a tag in the text field.", 
				"templateList", 
				wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 6).spacing(6, 6)
			.applyTo(parent);

		createProjectControls(parent, dbc);
		IObservableValue selectedEclipseProject = createEclipseProjectControls(parent, dbc);

		TabFolder tabContainer= new TabFolder(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(3, 1)
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.applyTo(tabContainer);
		tabContainer.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				// JBIDE-21072: force re-layout of the parent upon tab switching
				parent.layout(true, false);
			}
		});

		IObservableValue useLocalTemplateObservable = 
				BeanProperties.value(ITemplateListPageModel.PROPERTY_USE_LOCAL_TEMPLATE).observe(model);
		ValueBindingBuilder
			.bind(new TabFolderSelectionProperty().observe(tabContainer))
			.converting(new Converter(Integer.class, Boolean.class) {
				@Override
				public Object convert(Object fromObject) {
					return Integer.valueOf(LOCAL_TEMPLATE_TAB_INDEX).equals(fromObject);
				}
			})
			.to(useLocalTemplateObservable).converting(new Converter(Boolean.class, Integer.class) {
				@Override
				public Object convert(Object fromObject) {
					return (fromObject != null && (Boolean) fromObject) ? LOCAL_TEMPLATE_TAB_INDEX : 0;
				}
			})
			.in(dbc);

		IObservableValue serverTemplate = createServerTemplateControls(tabContainer, useLocalTemplateObservable, dbc);
		IObservableValue localTemplateFilename = createLocalTemplateControls(tabContainer, useLocalTemplateObservable, dbc);

		createDetailsGroup(parent, dbc);

		model.setUseLocalTemplate(false);

		// validate required template
		IObservableValue selectedTemplate = BeanProperties.value(ITemplateListPageModel.PROPERTY_SELECTED_TEMPLATE).observe(model);
		TemplateListPageValidator pageValidator = new TemplateListPageValidator(useLocalTemplateObservable, localTemplateFilename, serverTemplate, selectedTemplate, selectedEclipseProject, parent);
		dbc.addValidationStatusProvider(pageValidator );
		ControlDecorationSupport.create(pageValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}

	private IObservableValue createEclipseProjectControls(Composite parent, DataBindingContext dbc) {
		// existing project
		Label existingProjectLabel = new Label(parent, SWT.NONE);
		existingProjectLabel.setText("Use existing workspace project:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(existingProjectLabel);

		final Text existingProjectNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(existingProjectNameText);
		
		ISWTObservableValue projectNameTextObservable = WidgetProperties.text(SWT.Modify).observe(existingProjectNameText);

		IObservableValue eclipseProjectObservable = BeanProperties.value(
				ITemplateListPageModel.PROPERTY_ECLIPSE_PROJECT).observe(model);
		
		ValueBindingBuilder
			.bind(projectNameTextObservable)
			.converting(new Converter(String.class, org.eclipse.core.resources.IProject.class) {
				@Override
				public Object convert(Object fromObject) {
					String name = (String)fromObject;
					return ProjectUtils.getProject(name);
				}
			})
			.to(eclipseProjectObservable)
			.converting(new Converter(org.eclipse.core.resources.IProject.class, String.class) {
				
				@Override
				public Object convert(Object fromObject) {
					return fromObject == null?"": ((org.eclipse.core.resources.IProject)fromObject).getName();
				}
			})
			.in(dbc);

		// project name content assist
		ControlDecoration dec = new ControlDecoration(existingProjectNameText, SWT.TOP | SWT.RIGHT);
		
		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing a project name.");
		dec.setShowOnlyOnFocus(true);

		new AutoCompleteField(existingProjectNameText, new TextContentAdapter(), ProjectUtils.getAllOpenedProjects());

		// browse projects
		Button browseProjectsButton = new Button(parent, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.hint(100, SWT.DEFAULT)
				.grab(false, false)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects());
		
		Link gitLabel = new Link(parent, SWT.NONE);
		gitLabel.setText("The project needs to be <a>shared with Git</a> and have a remote repository accessible by OpenShift");
		gitLabel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						org.eclipse.core.resources.IProject p = model.getEclipseProject();
						model.setEclipseProject(null);
						EGitUIUtils.openGitSharingWizard(Display.getCurrent().getActiveShell(), p);
						model.setEclipseProject(p);//force re-validation
					}
				});
			}
		});
		GridDataFactory.fillDefaults().span(3, 1).applyTo(gitLabel);
		
		eclipseProjectObservable.addValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				org.eclipse.core.resources.IProject p = (org.eclipse.core.resources.IProject) event.getObservableValue().getValue();
				toggleEgitLink(gitLabel, p);
			}
		});
		toggleEgitLink(gitLabel, model.getEclipseProject());
		return projectNameTextObservable;
	}

	/**
	 * Open a dialog box to select an open project when clicking on the 'Browse' button.
	 * 
	 * @return
	 */
	private SelectionListener onBrowseProjects() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectExistingProjectDialog dialog = 
						new SelectExistingProjectDialog(model.getEclipseProject() == null? null: model.getEclipseProject().getName(), getShell());
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					model.setEclipseProject(((org.eclipse.core.resources.IProject) selectedProject));
				}
			}
		};
	}

	private void createProjectControls(Composite parent, DataBindingContext dbc) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText("OpenShift project: ");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(projectLabel);

		StructuredViewer projectsViewer = new ComboViewer(parent);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(projectsViewer.getControl());

		projectsViewer.setContentProvider(new ObservableListContentProvider());
		projectsViewer.setLabelProvider(new ObservableTreeItemLabelProvider());
		projectsViewer.setInput(
				BeanProperties.list(ITemplateListPageModel.PROPERTY_PROJECT_ITEMS).observe(model));

		IObservableValue selectedProjectObservable = ViewerProperties.singleSelection().observe(projectsViewer);
		Binding selectedProjectBinding =
				ValueBindingBuilder.bind(selectedProjectObservable)
					.converting(new ObservableTreeItem2ModelConverter(IProject.class))
					.validatingAfterConvert(new IValidator() {

						@Override
						public IStatus validate(Object value) {
							if (value instanceof IProject) {
								return ValidationStatus.ok();
							}
							return ValidationStatus.cancel("Please choose an OpenShift project.");
						}
					})
					.to(BeanProperties.value(ITemplateListPageModel.PROPERTY_PROJECT)
					.observe(model))
					.converting(new Model2ObservableTreeItemConverter(TemplateTreeItems.INSTANCE))
					.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		BeanProperties.value(ITemplateListPageModel.PROPERTY_CONNECTION).observe(model)
				.addValueChangeListener(onConnectionChanged());

		StyledText manageProjectsLink = StyledTextUtils.emulateLinkWidget("<a>Manage Projects</a>", new StyledText(parent, SWT.WRAP));
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).indent(8, 0)
			.applyTo(manageProjectsLink);
		manageProjectsLink.addListener(SWT.MouseDown, onManageProjectsClicked());

		Label filler = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).span(3, 1)
			.applyTo(filler);
	}

	private Listener onManageProjectsClicked() {
		return new Listener() {

			@Override
			public void handleEvent(Event event) {
				try {
					// run in job to enforce busy cursor which doesnt work otherwise
					WizardUtils.runInWizard(new UIUpdatingJob("Opening projects wizard...") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							return Status.OK_STATUS;
						}

						@Override
						protected IStatus updateUI(IProgressMonitor monitor) {
							ManageProjectsWizard manageProjectsWizard = new ManageProjectsWizard(model.getConnection());
							int result = new OkCancelButtonWizardDialog(getShell(), manageProjectsWizard).open();
							// reload projects to reflect changes that happened in projects wizard
							if (manageProjectsWizard.hasChanged()) {
								loadResources(templatesViewer, model);
							}
							if(Dialog.OK == result) {
								IProject selectedProject = manageProjectsWizard.getSelectedProject();
								if (selectedProject != null) {
									model.setProject(selectedProject);
								}
							};
							return Status.OK_STATUS;
						}

					}, getContainer());
				} catch (InvocationTargetException | InterruptedException e) {
					// swallow intentionnally
				}
			}
		};
	}

	private IObservableValue createLocalTemplateControls(TabFolder tabContainer, IObservableValue useLocalTemplate, DataBindingContext dbc) {

		TabItem localTemplatesTab = new TabItem(tabContainer, SWT.NONE);
		localTemplatesTab.setText("Local template");

		Composite parent = new Composite(tabContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10).spacing(6, 2)
			.applyTo(parent);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Select a local template:");
		GridDataFactory.fillDefaults().span(3,1).applyTo(lbl);
		
		// local template file name
		Text txtLocalTemplateFileName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(txtLocalTemplateFileName);
		IObservableValue localTemplateFilename = WidgetProperties.text(SWT.Modify).observe(txtLocalTemplateFileName);
		ValueBindingBuilder
				.bind(localTemplateFilename )
				.to(BeanProperties.value(
						ITemplateListPageModel.PROPERTY_LOCAL_TEMPLATE_FILENAME).observe(model))
				.validatingBeforeSet( o -> isFile(o.toString())?
						ValidationStatus.ok(): 
						ValidationStatus.error(txtLocalTemplateFileName.getText() +" is not a file"))
				.in(dbc);

		// browse button
		Button btnBrowseFiles = new Button(parent, SWT.NONE);
		btnBrowseFiles.setText("File system...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(btnBrowseFiles);

		btnBrowseFiles.addSelectionListener(onFileSystemBrowseClicked());
		
		// browse button
		Button btnBrowseWorkspaceFiles = new Button(parent, SWT.NONE);
		btnBrowseWorkspaceFiles.setText("Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT)
				.applyTo(btnBrowseWorkspaceFiles);

		btnBrowseWorkspaceFiles.addSelectionListener(onBrowseWorkspaceClicked());


		localTemplatesTab.setControl(parent);
		
		return localTemplateFilename;
	}

	private SelectionListener onBrowseWorkspaceClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = createFileDialog(model.getLocalTemplateFileName());
				if (dialog.open() == IDialogConstants.OK_ID && dialog.getFirstResult() instanceof IFile) {
					String path = ((IFile)dialog.getFirstResult()).getFullPath().toPortableString();
					String file = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", path);
					setLocalTemplate(file);
				}
			}


			private ElementTreeSelectionDialog createFileDialog(String selectedFile) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
						    getShell(),
						    new WorkbenchLabelProvider(),
						    new WorkbenchContentProvider()
				);
				dialog.setTitle("Select an OpenShift template");
				dialog.setMessage("Select an OpenShift template (*.json)");
				dialog.setInput( ResourcesPlugin.getWorkspace().getRoot() );
				dialog.addFilter(new ViewerFilter() {
					
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						return element instanceof IContainer 
								|| (element instanceof IFile && ((IFile)element).getFileExtension().equals("json"));
					}
				});
				dialog.setAllowMultiple( false );
				IResource res = model.getEclipseProject();
				if (StringUtils.isNotBlank(selectedFile)) {
					String prefix = "${workspace_loc:";
					String path = selectedFile;
					if (selectedFile.startsWith(prefix) && selectedFile.endsWith("}")) {
						path = path.substring(prefix.length(), path.length()-1);
					}
					res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				}
				if (res != null) {
					dialog.setInitialSelection(res);
				}
				
				return dialog;
			}
		};
	}


	private void setLocalTemplate(String file) {
		if (file == null || !isFile(file)) {
			return;
		}
		try {
			model.setLocalTemplateFileName(file);
			return;
		} catch (ClassCastException ex) {
			IStatus status = ValidationStatus.error(ex.getMessage(), ex);
			OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
			ErrorDialog.openError(getShell(), "Template Error",
					NLS.bind("The file \"{0}\" is not an OpenShift template.", 
							file),
					status);
		} catch (UnsupportedVersionException ex) {
			IStatus status = ValidationStatus.error(ex.getMessage(), ex);
			OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
			ErrorDialog.openError(getShell(), "Template Error", 
					NLS.bind("The file \"{0}\" is a template in a version that we do not support.", file),
					status);
		} catch (OpenShiftException ex) {
			IStatus status = ValidationStatus.error(ex.getMessage(), ex);
			OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
			ErrorDialog.openError(getShell(), "Template Error",
					NLS.bind("Unable to read and/or parse the file \"{0}\" as a template.", file),
					status);
		}
	}
	
	private boolean isFile(String path) {
		return Files.isRegularFile(Paths.get(substituteVariables(path)));
	}

	private IObservableValue createServerTemplateControls(TabFolder tabFolder, IObservableValue uploadTemplate, DataBindingContext dbc) {

		TabItem serverTemplatesTab = new TabItem(tabFolder, SWT.NONE);
		serverTemplatesTab.setText("Server templates");

		Composite parent = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults()
		.margins(10, 6).spacing(2, 2)
		.applyTo(parent);

		serverTemplatesTab.setControl(parent);

		// filter text
		final Text txtTemplateFilter = UIUtils.createSearchText(parent);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(txtTemplateFilter);

		IObservableValue eclipseProjectObservable = BeanProperties.value(ITemplateListPageModel.PROPERTY_ECLIPSE_PROJECT).observe(model);
		eclipseProjectObservable.addValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				filterTemplates(txtTemplateFilter, (org.eclipse.core.resources.IProject)event.getObservableValue().getValue());
			}
		});
		
		filterTemplates(txtTemplateFilter, model.getEclipseProject());
		
		// the list of templates
		this.templatesViewer = createServerTemplatesViewer(parent, txtTemplateFilter);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).hint(400, 180)
				.applyTo(templatesViewer.getControl());
		
		IObservableValue selectedViewerServerTemplate = 
				ViewerProperties.singleSelection().observe(templatesViewer);
		ValueBindingBuilder
			.bind(selectedViewerServerTemplate)
			.converting(new ObservableTreeItem2ModelConverter(ITemplate.class))
			.to(BeanProperties.value(ITemplateListPageModel.PROPERTY_SERVER_TEMPLATE).observe(model))
			.converting(new Model2ObservableTreeItemConverter(TemplateTreeItems.INSTANCE))
			.in(dbc);

		templatesViewer.addDoubleClickListener(onServerTemplateDoubleClicked());

		txtTemplateFilter.addModifyListener(onFilterTextTyped(templatesViewer));

		return selectedViewerServerTemplate;
	}

	protected String findMatchingTags(org.eclipse.core.resources.IProject project) {
		return new ProjectBuilderTypeDetector().findTemplateFilter(project);
	}

	private void createDetailsGroup(Composite parent, DataBindingContext dbc) {

		// details
		Group detailsGroup = new Group(parent, SWT.NONE);
		detailsGroup.setText("Template details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.grab(true, false)
				.span(3, 1)
				.hint(SWT.DEFAULT, 106)
				.applyTo(detailsGroup);
		GridLayoutFactory.fillDefaults()
			.margins(10, 6).spacing(2, 2) //TODO fix margins
			.applyTo(detailsGroup);

		Composite detailsContainer = new Composite(detailsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(detailsContainer);

		new TemplateDetailViews(
			BeanProperties.value(ITemplateListPageModel.PROPERTY_SELECTED_TEMPLATE).observe(model), null, detailsContainer, dbc)
			.createControls();

		// detail resources button
		Button btnDetails = new Button(detailsGroup, SWT.NONE);
		btnDetails.setText("Defined Resources...");
		GridDataFactory.fillDefaults()
				.align(SWT.RIGHT, SWT.CENTER)
				.applyTo(btnDetails);

		IObservableValue selectedTemplate =
				BeanProperties.value(ITemplateListPageModel.PROPERTY_SELECTED_TEMPLATE).observe(model);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnDetails))
				.notUpdatingParticipant()
				.to(selectedTemplate)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		btnDetails.addSelectionListener(onDefinedResourcesClicked());


	}

	private IDoubleClickListener onServerTemplateDoubleClicked() {
		return new IDoubleClickListener() {
	        @Override
	        public void doubleClick(DoubleClickEvent event) {
	            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
	            if (hasTemplate(selection.getFirstElement())
	            		&& canFlipToNextPage()) {
	            	getContainer().showPage(getNextPage());
	            }
	        }
	    };
	}

	private TreeViewer createServerTemplatesViewer(Composite parent, final Text templateFilterText) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] {
						BeanProperties.list(ITemplateListPageModel.PROPERTY_TEMPLATES),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider = new ObservableListTreeContentProvider(
				childrenProperty.listFactory(), null);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new ObservableTreeItemStyledCellLabelProvider());
		viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		viewer.setComparator(new TemplateComparator());
		viewer.addFilter(new TemplateViewerFilter(templateFilterText));
		viewer.setInput(model);

		return viewer;
	}

	private static class TemplateViewerFilter extends ViewerFilter {

		private Text filterText;

		public TemplateViewerFilter(Text filterText) {
			this.filterText = filterText;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!(element instanceof ObservableTreeItem)
					|| !(((ObservableTreeItem) element).getModel() instanceof ITemplate)) {
				return true;
			}
			ITemplate template = (ITemplate) ((ObservableTreeItem) element).getModel();
			return ResourceUtils.isMatching(filterText.getText(), template);
		}
	}

	private SelectionAdapter onFileSystemBrowseClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = createFileDialog(model.getLocalTemplateFileName());
				String file = dialog.open();
				setLocalTemplate(file);
			}

			private FileDialog createFileDialog(String selectedFile) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setText("Select an OpenShift template");
				if(StringUtils.isNotBlank(selectedFile)) {
					File file = new File(selectedFile);
					dialog.setFilterPath(file.getParentFile().getAbsolutePath());
				}
				return dialog;
			}
		};
	}

	private SelectionAdapter onDefinedResourcesClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ITemplate template = model.getSelectedTemplate();
				new ResourceSummaryDialog(getShell(),
						template.getItems(),
						"Template Details",
						NLS.bind("The following resources will be created by using template\n\"{0}\":", template.getName()),
						new ResourceDetailsLabelProvider(), new ResourceDetailsContentProvider()).open();
			}

		};
	}

	private ModifyListener onFilterTextTyped(final TreeViewer viewer) {
		return new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				viewer.refresh();
				viewer.expandAll();
			}
		};
	}

	private IValueChangeListener onConnectionChanged() {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				loadResources(templatesViewer, model);
				templatesViewer.expandAll();
			}
		};
	}

	private void loadResources(final TreeViewer templatesViewer, final ITemplateListPageModel model) {
		if (!model.hasConnection()) {
			return;
		}
		try {
			Job jobs = new JobChainBuilder(
					new AbstractDelegatingMonitorJob("Loading projects, templates...") {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							model.loadResources();
							return Status.OK_STATUS;
						}
					}).runWhenSuccessfullyDone(new UIJob("Verifying required project...") {

						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							if(!model.hasProjects()) {
								List<IProject> projects = new ObservableTreeItem2ModelConverter().convert(model.getProjectItems());
								Connection connection = model.getConnection();
								NewProjectWizard newProjectWizard = new NewProjectWizard(connection, projects);
								if (Dialog.CANCEL ==
										WizardUtils.openWizardDialog(newProjectWizard, getShell())) {
									WizardUtils.close(getWizard());
									return Status.CANCEL_STATUS;
								} else {
									model.loadResources();
									model.setProject(newProjectWizard.getProject());
								}
							}
							return Status.OK_STATUS;
						}
					}).runWhenSuccessfullyDone(new UIJob("Expanding resource tree...") {

						@Override
						public IStatus runInUIThread(IProgressMonitor monitor) {
							templatesViewer.expandAll();
							return Status.OK_STATUS;
						}
					}).build();
			WizardUtils.runInWizard(jobs, getContainer());
		} catch (InvocationTargetException | InterruptedException e) {
			// intentionnally swallowed
		}
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		loadResources(templatesViewer, model);
		// fix GTK3 combo boxes too small
		// https://issues.jboss.org/browse/JBIDE-16877,
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=431425
		((Composite) getControl()).layout(true, true);
	}

	public static class TemplateComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (hasTemplate(e1) && hasTemplate(e2)) {
				String t1 = ((ITemplate)((ObservableTreeItem)e1).getModel()).getName();
				String t2 = ((ITemplate)((ObservableTreeItem)e2).getModel()).getName();
				return t1.compareTo(t2);
			}
			return super.compare(viewer, e1, e2);
		}
	}

	private static boolean hasTemplate(Object item) {
		return item instanceof ITemplate ||
			   item instanceof ObservableTreeItem &&
			   ((ObservableTreeItem)item).getModel() instanceof ITemplate;
	}

	/**
	 * A validator that validates this page based on the choice to use a local
	 * or server template and the settings that are required therefore.
	 */
	private class TemplateListPageValidator extends MultiValidator {

		private IObservableValue useLocalTemplate;
		private IObservableValue localTemplateFilename;
		private IObservableValue serverTemplate;
		private IObservableValue selectedTemplate;
		private IObservableValue projectNameObservable;

		private IObservableList mutableTargets = new WritableList<>();
		private Composite composite;
		
		public TemplateListPageValidator(IObservableValue useLocalTemplate, IObservableValue localTemplateFilename, 
				IObservableValue serverTemplate, IObservableValue selectedTemplate, IObservableValue projectNameObservable, Composite composite) {
			this.useLocalTemplate = useLocalTemplate;
			useLocalTemplate.getValue();
			this.localTemplateFilename = localTemplateFilename;
			this.serverTemplate = serverTemplate;
			this.selectedTemplate = selectedTemplate;
			this.projectNameObservable = projectNameObservable;
			this.composite = composite;
		}

		@Override
		protected IStatus validate() {
			IStatus status = ValidationStatus.ok();
			mutableTargets.clear();
			
			final String projectName = (String) projectNameObservable.getValue();

			if (!StringUtils.isEmpty(projectName)) {
				final org.eclipse.core.resources.IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				if (!ProjectUtils.exists(project)) {
					status = ValidationStatus.error(
							NLS.bind("The project {0} does not exist in your workspace.", projectName));
				} else if (!ProjectUtils.isAccessible(project)) {
					status = ValidationStatus.error(
							NLS.bind("The project {0} is not open.", projectName));
				} else if (EGitUtils.isSharedWithGit(project)){
					try {
						List<String> repos = EGitUtils.getRemoteGitRepos(project);
						if (repos == null || repos.isEmpty()) {
							status = ValidationStatus.error(
									NLS.bind("No remote Git repository is defined on project {0}", projectName));
						} else {
							status = getGitDirtyStatus(project);						
						}
					} catch (CoreException e) {
						status = ValidationStatus.error(
								NLS.bind("Can not read Git config on project {0} : {1}", projectName, e.getMessage()));
					}
				} else {
					status = ValidationStatus.error(
							NLS.bind("The project {0} is not shared with Git.", projectName));
				}
			}
			if (!status.isOK()) {
				mutableTargets.add(projectNameObservable);
			} else {
				if (Boolean.TRUE.equals(useLocalTemplate.getValue())) {
					String localTemplate = (String)localTemplateFilename.getValue();
					if (StringUtils.isNotEmpty(localTemplate)){ 
						if (!isFile(localTemplate)) {
							status = ValidationStatus.error(NLS.bind("{0} is not a valid file.", localTemplate));
							mutableTargets.add(localTemplateFilename);
						}
					} else if (selectedTemplate.getValue() == null) {
						status = ValidationStatus.cancel("Please select a local template file.");
						mutableTargets.add(localTemplateFilename);
					}
				} else {
					if (selectedTemplate.getValue() == null){
						status = ValidationStatus.cancel("Please select a server template.");
						mutableTargets.add(serverTemplate);
					} 
				}
			}			
			// force redraw since removed decorations somehow stay visible, GTK3 bug?
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
			composite.redraw();

			return status;
		}

		@Override
		public IObservableList getTargets() {
			return mutableTargets;
		}
	}
	
	private IStatus getGitDirtyStatus(org.eclipse.core.resources.IProject project) {
		try {
			if (EGitUtils.isDirty(project, false, new NullProgressMonitor())) {
				return ValidationStatus.error(NLS.bind(
						"The project {0} has uncommitted changes. Please commit those changes first.",
						project.getName()));
			} else {
				return ValidationStatus.ok();
			}
		} catch (NoWorkTreeException | IOException | GitAPIException e) {
			return ValidationStatus.error(NLS.bind(
					"The git repository for project {0} looks corrupt. Please fix it before using it.",
					project.getName()));
		}
	}

	private void filterTemplates(Text text, org.eclipse.core.resources.IProject project) {
		String tags = findMatchingTags(project);
		if (tags != null && !text.isDisposed()) {
			text.setText(tags);
		}
	}

	private void toggleEgitLink(Link gitLabel, org.eclipse.core.resources.IProject p) {
		if (gitLabel.isDisposed()) {
			return;
		}
		boolean showLink = p != null && !EGitUtils.isSharedWithGit(p);
		UIUtils.setVisibleAndExclude(showLink, gitLabel);
	}

	private String substituteVariables(String string) {
		try {
			return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(string);
		} catch (CoreException ex) {
			throw new RuntimeException(ex);
		}
	}
}
