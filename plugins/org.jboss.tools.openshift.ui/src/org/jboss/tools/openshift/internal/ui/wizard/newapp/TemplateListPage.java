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
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.IValidator;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
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

import com.openshift.restclient.ResourceFactoryException;
import com.openshift.restclient.UnsupportedVersionException;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list templates to a user
 *
 * @author jeff.cantrill
 *
 */
public class TemplateListPage  extends AbstractOpenShiftWizardPage  {

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

		SelectObservableValue uploadTemplate = new SelectObservableValue();
		ValueBindingBuilder
			.bind(uploadTemplate)
			.to(BeanProperties.value(
					ITemplateListPageModel.PROPERTY_USE_UPLOAD_TEMPLATE).observe(model))
			.in(dbc);
		createUploadControls(parent, uploadTemplate, dbc);
		createServerTemplateControls(parent, uploadTemplate, dbc);
		model.setUseUploadTemplate(false);
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

		StyledText manageProjectsLink = new StyledText(parent, SWT.WRAP);
		StyledTextUtils.setTransparent(manageProjectsLink);
		StyledTextUtils.setLinkText("<a>Manage Projects</a>", manageProjectsLink);
		manageProjectsLink.setEditable(false);
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
							if(Dialog.OK == new OkCancelButtonWizardDialog(getShell(), manageProjectsWizard).open()) {
								IProject selectedProject = manageProjectsWizard.getSelectedProject();
								if (selectedProject != null) {
									model.setProject(selectedProject);
								}
							};
							// reload projects to reflect changes that happened in projects wizard
							loadResources(templatesViewer, model);
							return Status.OK_STATUS;
						}
											
					}, getContainer());
				} catch (InvocationTargetException | InterruptedException e) {
					// swallow intentionnally
				}
			}
		};
	}

	private void createUploadControls(Composite parent, SelectObservableValue uploadTemplate, DataBindingContext dbc) {
		// upload  app radio
		Button btnUploadTemplate = new Button(parent, SWT.RADIO);
		btnUploadTemplate.setText("Use a template from my local file system:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(3, 1).grab(true, false)
				.applyTo(btnUploadTemplate);

		uploadTemplate.addOption(Boolean.TRUE, 
				WidgetProperties.selection().observe(btnUploadTemplate));

		// uploaded file name
		Text txtUploadedFileName = new Text(parent, SWT.BORDER);
		txtUploadedFileName.setEnabled(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false)
				.applyTo(txtUploadedFileName);
		IObservableValue uploadedFilenameTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(txtUploadedFileName);
		ValueBindingBuilder
				.bind(uploadedFilenameTextObservable)
				.to(BeanProperties.value(
						ITemplateListPageModel.PROPERTY_TEMPLATE_FILENAME).observe(model))
				.in(dbc);

		// browse button
		Button btnBrowseFiles = new Button(parent, SWT.NONE);
		btnBrowseFiles.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(120, SWT.DEFAULT).indent(6, 0)
				.applyTo(btnBrowseFiles);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnBrowseFiles))
				.notUpdatingParticipant()
				.to(uploadTemplate)
				.in(dbc);
		btnBrowseFiles.addSelectionListener(onBrowseClicked());
	}

	private void createServerTemplateControls(Composite parent, SelectObservableValue uploadTemplate, DataBindingContext dbc) {
		// existing app radio
		Button btnServerTemplate = new Button(parent, SWT.RADIO);
		btnServerTemplate.setText("Use a template from the server:");
		GridDataFactory.fillDefaults()
				.span(3, 1).indent(0, 8).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(btnServerTemplate);

		uploadTemplate.addOption(Boolean.FALSE, 
				WidgetProperties.selection().observe(btnServerTemplate));
		
		Composite treeComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(treeComposite);
		GridLayoutFactory.fillDefaults()
			.spacing(2, 2)
			.applyTo(treeComposite);

		// filter text
		Text txtTemplateFilter = UIUtils.createSearchText(treeComposite);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER)
				.applyTo(txtTemplateFilter);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(txtTemplateFilter))
			.notUpdatingParticipant()
			.to(uploadTemplate)
			.converting(new InvertingBooleanConverter())
			.in(dbc);		
		
		// the list of templates
		this.templatesViewer = createTemplatesViewer(treeComposite, txtTemplateFilter);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).hint(400, 180)
				.applyTo(templatesViewer.getControl());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(templatesViewer.getControl()))
			.notUpdatingParticipant()
			.to(uploadTemplate)
			.converting(new InvertingBooleanConverter())
			.in(dbc);

		IObservableValue selectedItem = 
				ViewerProperties.singleSelection().observe(templatesViewer);
		ValueBindingBuilder
			.bind(selectedItem)
			.converting(new ObservableTreeItem2ModelConverter(ITemplate.class))
			.validatingAfterConvert(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (!(value instanceof ITemplate)) {
						return ValidationStatus.cancel("Please select a template to create your application.");
					}
					return ValidationStatus.ok();
					
				}
			})
			.to(BeanProperties.value(ITemplateListPageModel.PROPERTY_TEMPLATE).observe(model))
			.converting(new Model2ObservableTreeItemConverter(TemplateTreeItems.INSTANCE))
			.in(dbc);

		txtTemplateFilter.addModifyListener(onFilterTextTyped(templatesViewer));

		IObservableValue selectedResource = new WritableValue();
		ValueBindingBuilder
			.bind(selectedItem)
			.converting(new ObservableTreeItem2ModelConverter())
			.to(selectedResource)
			.notUpdatingParticipant()
			.in(dbc);
				
		// details
		final Group detailsGroup = new Group(treeComposite, SWT.NONE);
		detailsGroup.setText("Details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.hint(SWT.DEFAULT, 106)
				.applyTo(detailsGroup);
		GridLayoutFactory.fillDefaults()
			.margins(10, 6).spacing(2, 2)
			.applyTo(detailsGroup);
		
		
		Composite detailsContainer = new Composite(detailsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 140)
			.applyTo(detailsContainer);
		new TemplateDetailViews(selectedResource, null, detailsContainer, dbc).createControls();
		
		// details button
		Button btnDetails = new Button(detailsGroup, SWT.NONE);
		btnDetails.setText("Defined Resources...");
		GridDataFactory.fillDefaults()
				.align(SWT.RIGHT, SWT.CENTER).span(2,1).grab(false, false)
				.applyTo(btnDetails);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnDetails))
				.notUpdatingParticipant()
				.to(selectedResource)
				.converting(new Converter(ITemplate.class, Boolean.class) {
					@Override
					public Object convert(Object value) {
						if(!(value instanceof ITemplate)) {
							return Boolean.FALSE;
						}
						return Boolean.TRUE;
					}
				})
				.in(dbc);
		btnDetails.addSelectionListener(onDetailsClicked());
	}

	private TreeViewer createTemplatesViewer(Composite parent, final Text templateFilterText) {
		TreeViewer viewer = new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] { 
						BeanProperties.list(ITemplateListPageModel.PROPERTY_PROJECT_ITEMS), 
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider = new ObservableListTreeContentProvider(
				childrenProperty.listFactory(), null);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new ObservableTreeItemStyledCellLabelProvider());
		viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		viewer.setComparator(new ViewerComparator());
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
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				if(StringUtils.isNotBlank(model.getTemplateFileName())) {
					File file = new File(model.getTemplateFileName());
					dialog.setFilterPath(file.getParentFile().getAbsolutePath());
				}
				String file = null;
				do {
					file = dialog.open();
					if (file != null) {
						try {
							model.setTemplateFileName(file);
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
									NLS.bind("The file \"{0}\" is a template in a version that we do not support.", 
											file),
									status);
						} catch (ResourceFactoryException ex) {
							IStatus status = ValidationStatus.error(ex.getMessage(), ex);
							OpenShiftUIActivator.getDefault().getLogger().logStatus(status);
							ErrorDialog.openError(getShell(), "Template Error",
									NLS.bind("Unable to read and/or parse the file \"{0}\" as a template.",
											file),
									status);
						}
					}
				} while (file != null);
			}
		};
	}

	private SelectionAdapter onDetailsClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final String message = "The following resources will be created by using this template:";
				new ResourceSummaryDialog(getShell(), 
						model.getTemplate().getItems(), 
						"Template Details",
						message, 
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
								if (Dialog.CANCEL == 
										WizardUtils.openWizardDialog(new NewProjectWizard(connection, projects), getShell())) {
									WizardUtils.close(getWizard());
									return Status.CANCEL_STATUS;
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


}
