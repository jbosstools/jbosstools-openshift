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
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
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
				"Templates choices may be reduced to a smaller list by typing the name of a tag in the text field.",
				"templateList",
				wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 6).spacing(2, 2)
			.applyTo(parent);

		createProjectControls(parent, dbc);

		TabFolder tabContainer= new TabFolder(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
		.span(3, 1)
		.align(SWT.FILL, SWT.CENTER)
		.grab(true, false)
		.applyTo(tabContainer);

		SelectObservableValue useLocalTemplate = new SelectObservableValue();
		ValueBindingBuilder
			.bind(useLocalTemplate)
			.to(BeanProperties.value(
					ITemplateListPageModel.PROPERTY_USE_LOCAL_TEMPLATE).observe(model))
			.in(dbc);

		ValueBindingBuilder
		  .bind(new TabFolderSelectionProperty().observe(tabContainer))
		  .converting(new Converter(Integer.class, Boolean.class) {
			@Override
			public Object convert(Object fromObject) {
				return Integer.valueOf(LOCAL_TEMPLATE_TAB_INDEX).equals(fromObject);
			}
		  })
		  .to(BeanProperties.value(
					ITemplateListPageModel.PROPERTY_USE_LOCAL_TEMPLATE).observe(model))
		  .converting(new Converter(Boolean.class,Integer.class) {
			@Override
			public Object convert(Object fromObject) {
					return (fromObject != null && (Boolean)fromObject)?LOCAL_TEMPLATE_TAB_INDEX:0;
			}
		})
		 .in(dbc);


		IObservableValue serverTemplate = createServerTemplateControls(tabContainer, useLocalTemplate, dbc);
		IObservableValue localTemplateFilename = createLocalTemplateControls(tabContainer, useLocalTemplate, dbc);

		createDetailsGroup(parent, dbc);

		model.setUseLocalTemplate(false);

		// validate required template
		IObservableValue selectedTemplate = BeanProperties.value(ITemplateListPageModel.PROPERTY_SELECTED_TEMPLATE).observe(model);
		SelectedTemplateValidator selectedTemplateValidator = new SelectedTemplateValidator(useLocalTemplate, localTemplateFilename, serverTemplate, selectedTemplate, parent);
		dbc.addValidationStatusProvider(selectedTemplateValidator );
		ControlDecorationSupport create = ControlDecorationSupport.create(
				selectedTemplateValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}

	private IValueChangeListener onRequiredValueMissing() {
		return null;
	}

	private void createProjectControls(Composite parent, DataBindingContext dbc) {
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText("Project: ");
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
							return ValidationStatus.cancel("Please choose a project.");
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

	private IObservableValue createLocalTemplateControls(TabFolder tabContainer, SelectObservableValue useLocalTemplate, DataBindingContext dbc) {

		TabItem localTemplatesTab = new TabItem(tabContainer, SWT.NONE);
		localTemplatesTab.setText("Local template");

		Composite parent = new Composite(tabContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults()
		.numColumns(3).margins(10, 6).spacing(2, 2)
		.applyTo(parent);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Select a template from the file system:");

		// local template file name
		Text txtLocalTemplateFileName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(txtLocalTemplateFileName);
		IObservableValue localTemplateFilename = WidgetProperties.text(SWT.Modify).observe(txtLocalTemplateFileName);
		Binding localTemplateFilenameBinding = ValueBindingBuilder
				.bind(localTemplateFilename )
				.to(BeanProperties.value(
						ITemplateListPageModel.PROPERTY_LOCAL_TEMPLATE_FILENAME).observe(model))
				.in(dbc);

		// browse button
		Button btnBrowseFiles = new Button(parent, SWT.NONE);
		btnBrowseFiles.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(120, SWT.DEFAULT).indent(6, 0)
				.applyTo(btnBrowseFiles);

		btnBrowseFiles.addSelectionListener(onBrowseClicked());

		localTemplatesTab.setControl(parent);

		return localTemplateFilename;
	}

	private IObservableValue createServerTemplateControls(TabFolder tabFolder, SelectObservableValue uploadTemplate, DataBindingContext dbc) {

		TabItem serverTemplatesTab = new TabItem(tabFolder, SWT.NONE);
		serverTemplatesTab.setText("Server templates");

		Composite parent = new Composite(tabFolder, SWT.NONE);
		GridLayoutFactory.fillDefaults()
		.margins(10, 6).spacing(2, 2)
		.applyTo(parent);

		serverTemplatesTab.setControl(parent);

		// filter text
		Text txtTemplateFilter = UIUtils.createSearchText(parent);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(txtTemplateFilter);

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

	private SelectionAdapter onBrowseClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = createFileDialog(model.getLocalTemplateFileName());
				String file = null;
				do {
					file = dialog.open();
					if (file != null) {
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
							file = null;
						} catch (UnsupportedVersionException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error",
									NLS.bind("The file \"{0}\" is a template in a version that we do not support.", file),
									status);
							file = null;
						} catch (OpenShiftException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error",
									NLS.bind("Unable to read and/or parse the file \"{0}\" as a template.", file),
									status);
							file = null;
						}
					}
				} while (file != null);
			}

			private FileDialog createFileDialog(String selectedFile) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
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
	private class SelectedTemplateValidator extends MultiValidator {

		private IObservableValue useLocalTemplate;
		private IObservableValue localTemplateFilename;
		private IObservableValue serverTemplate;
		private IObservableValue selectedTemplate;

		private IObservableList mutableTargets = new WritableList<>();
		private Composite composite;

		public SelectedTemplateValidator(IObservableValue useLocalTemplate, IObservableValue localTemplateFilename,
				IObservableValue serverTemplate, IObservableValue selectedTemplate, Composite composite) {
			this.useLocalTemplate = useLocalTemplate;
			this.localTemplateFilename = localTemplateFilename;
			this.serverTemplate = serverTemplate;
			this.selectedTemplate = selectedTemplate;
			this.composite = composite;
		}

		@Override
		protected IStatus validate() {
			IStatus status = ValidationStatus.ok();
			mutableTargets.clear();
			if (Boolean.TRUE.equals(useLocalTemplate.getValue())) {
				if (StringUtils.isEmpty((String) localTemplateFilename.getValue())
						|| selectedTemplate.getValue() == null) {
					status = ValidationStatus.cancel("Please select a local template file.");
					mutableTargets.add(localTemplateFilename);
				}
			} else {
				if (selectedTemplate.getValue() == null){
					status = ValidationStatus.cancel("Please select a server template.");
					mutableTargets.add(serverTemplate);
				}
			}

			// force redraw since removed decorations somehow stay visible, GTK3 bug?
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=478618
			composite.redraw();

			return status;
		}


	}


}
