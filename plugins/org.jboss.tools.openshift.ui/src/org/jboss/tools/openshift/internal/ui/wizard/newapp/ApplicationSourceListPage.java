/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.server.adapter.ProjectBuilderTypeDetector;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.SelectExistingProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.SelectProjectComponentBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.databinding.TabFolderSelectionProperty;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemStyledCellLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.common.AbstractProjectPage;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.UnsupportedVersionException;
import com.openshift.restclient.model.template.ITemplate;

/**
 * A page that offers a list of builder images or templates to a user
 *
 * @author jeff.cantrill
 * @author Andre Dietisheim
 * @author Jeff Maury
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ApplicationSourceListPage extends AbstractProjectPage<IApplicationSourceListPageModel> 
										implements IValueChangeListener<Object> {

	private static final int LOCAL_TEMPLATE_TAB_INDEX = 1;
	public static final String PAGE_NAME = "appSourceList";

	private TreeViewer templatesViewer;

	public ApplicationSourceListPage(IWizard wizard, IApplicationSourceListPageModel model) {
		super(wizard, model, "Select template", 
				"Server template choices may be filtered by typing the name of a tag in the text field.", 
				"templateList");
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
	    super.doCreateControls(parent, dbc);

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
				BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_USE_LOCAL_APP_SOURCE).observe(model);
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

		TabFolderTraverseListener tabFolderTraverseListener = new TabFolderTraverseListener(tabContainer);

		IObservableValue serverTemplate = 
				createServerTemplateControls(tabContainer, tabFolderTraverseListener, useLocalTemplateObservable, dbc);
		IObservableValue localTemplateFilename = 
				createLocalTemplateControls(tabContainer, tabFolderTraverseListener, useLocalTemplateObservable, dbc);
		/*
		 * listen for change on local template file name, server app and use local flag to trigger
		 * template loading. Also add a status validation provider bound to the app status of the model
		 * to enable/disable page buttons based on the status
		 */
		BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_LOCAL_APP_SOURCE_FILENAME).observe(model)
				.addValueChangeListener(this);
		BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SERVER_APP_SOURCE).observe(model)
				.addValueChangeListener(this);
		BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_USE_LOCAL_APP_SOURCE).observe(model)
				.addValueChangeListener(this);
        dbc.addValidationStatusProvider(new MultiValidator() {
            @Override
            protected IStatus validate() {
				return (IStatus) BeanProperties.value(
						IApplicationSourceListPageModel.PROPERTY_APP_SOURCE_STATUS, IStatus.class).observe(model).getValue();
			}
        });

		createDetailsGroup(parent, dbc);
		model.setUseLocalAppSource(false);

		// validate required template
		IObservableValue selectedTemplate = 
				BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE).observe(model);
		TemplateListPageValidator pageValidator = 
				new TemplateListPageValidator(useLocalTemplateObservable, localTemplateFilename, serverTemplate, selectedTemplate, 
						selectedEclipseProject, parent);
		dbc.addValidationStatusProvider(pageValidator );
		ControlDecorationSupport.create(pageValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		ProjectNameValidator projectNameValidator = new ProjectNameValidator(selectedEclipseProject, parent);
		dbc.addValidationStatusProvider(projectNameValidator);
		ControlDecorationSupport.create(projectNameValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}

	private IObservableValue createEclipseProjectControls(Composite parent, DataBindingContext dbc) {
		IObservableValue eclipseProjectObservable = 
				BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_ECLIPSE_PROJECT).observe(model);
		SelectProjectComponentBuilder builder = new SelectProjectComponentBuilder();
		builder
			.setTextLabel("Use existing workspace project:")
			.setHorisontalSpan(1)
			.setRequired(false)
			.setEclipseProjectObservable(eclipseProjectObservable)
			.setSelectionListener(onBrowseProjects())
			.build(parent, dbc);
		
		Link gitLabel = new Link(parent, SWT.NONE);
		gitLabel.setText("The project needs to be <a>shared with Git</a> and have a remote repository accessible by OpenShift");
		gitLabel.addSelectionListener(onClickEGitLink());
		GridDataFactory.fillDefaults().span(3, 1).applyTo(gitLabel);
		
		DataBindingUtils.addDisposableValueChangeListener(
				new IValueChangeListener() {

					@Override
					public void handleValueChange(ValueChangeEvent event) {
						org.eclipse.core.resources.IProject p = 
								(org.eclipse.core.resources.IProject) event.getObservableValue().getValue();
						toggleEgitLink(gitLabel, p);
					}
				}, eclipseProjectObservable, gitLabel);
		toggleEgitLink(gitLabel, model.getEclipseProject());
		return builder.getProjectNameTextObservable();
	}

	private SelectionAdapter onClickEGitLink() {
		return new SelectionAdapter() {
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
		};
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
					new SelectExistingProjectDialog(model.getEclipseProject() == null? 
							"Select an existing workspace project from the list below."
							: NLS.bind("Currently project {0} is selected, you may pick a different one from the list below.", 
									model.getEclipseProject().getName())
						, getShell());
				if (model.getEclipseProject() != null) {
					dialog.setInitialSelections(new Object[]{model.getEclipseProject()});
				}
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					model.setEclipseProject(((org.eclipse.core.resources.IProject) selectedProject));
				}
			}
		};
	}

	private IObservableValue createLocalTemplateControls(TabFolder tabContainer, TabFolderTraverseListener tabFolderTraverseListener, IObservableValue useLocalTemplate, DataBindingContext dbc) {

		TabItem localTemplatesTab = new TabItem(tabContainer, SWT.NONE);
		localTemplatesTab.setText("Local template");

		Composite parent = new Composite(tabContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 10).spacing(6, 2)
			.applyTo(parent);

		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText("Select a local template file or a full URL:");
		GridDataFactory.fillDefaults().span(3,1).applyTo(lbl);
		
		// local template file name
		Text txtLocalTemplateFileName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false)
				.span(3, 1)
				.applyTo(txtLocalTemplateFileName);
		IObservableValue localTemplateFilename = WidgetProperties.text(SWT.Modify).observe(txtLocalTemplateFileName);
		IValidator validator = (o -> {
		    IStatus status = ValidationStatus.ok();
		    if (!OpenshiftUIConstants.URL_VALIDATOR.isValid(o.toString()) 
		    		&& StringUtils.isNotBlank(o.toString()) 
		    		&& !isFile(o.toString())) {
		        status = ValidationStatus.error(o +" is not a file");
			}
			return status;
		});
		ValueBindingBuilder
				.bind(localTemplateFilename)
				.validatingBeforeSet(validator)
				.to(BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_LOCAL_APP_SOURCE_FILENAME).observe(model))
				.in(dbc);

		// browse button
		Button btnBrowseFiles = new Button(parent, SWT.NONE);
		btnBrowseFiles.setText("Browse File System...");
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.span(2,1)
				.grab(true, false)
				.applyTo(btnBrowseFiles);

		btnBrowseFiles.addSelectionListener(onFileSystemBrowseClicked());
		
		// browse button
		Button btnBrowseWorkspaceFiles = new Button(parent, SWT.NONE);
		btnBrowseWorkspaceFiles.setText("Browse Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.applyTo(btnBrowseWorkspaceFiles);
		btnBrowseWorkspaceFiles.addSelectionListener(onBrowseWorkspaceClicked());
		UIUtils.setEqualButtonWidth(btnBrowseFiles, btnBrowseWorkspaceFiles);
		localTemplatesTab.setControl(parent);
		tabFolderTraverseListener.bindTabControls(tabContainer.getItemCount() - 1,
				txtLocalTemplateFileName, btnBrowseFiles, btnBrowseWorkspaceFiles);
		return localTemplateFilename;
	}

	private SelectionListener onBrowseWorkspaceClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = UIUtils.createFileDialog(model.getLocalAppSourceFileName(),
				                                                             "Select an OpenShift template",
				                                                             "Select an OpenShift template (*.json)",
				                                                             "json",
				                                                             model.getEclipseProject());
				if (dialog.open() == IDialogConstants.OK_ID && dialog.getFirstResult() instanceof IFile) {
					String path = ((IFile)dialog.getFirstResult()).getFullPath().toString();
					String file = VariablesHelper.addWorkspacePrefix(path);
					setLocalTemplate(file);
				}
			}
		};
	}


	private void setLocalTemplate(String file) {
		if (file == null || !isFile(file)) {
			return;
		}
		try {
			model.setLocalAppSourceFileName(file);
			return;
		} catch (NotATemplateException ex) {
			MessageDialog.openWarning(getShell(), "Template Error",
					NLS.bind("The file \"{0}\" is not an OpenShift template. It contains a resource of type {1} instead.",
							file, ex.getResourceKind()));
		} catch (ClassCastException ex) {
			//should not happen due to NotATemplateException.
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
	
	private IObservableValue createServerTemplateControls(TabFolder tabFolder, TabFolderTraverseListener tabFolderTraverseListener, IObservableValue uploadTemplate, DataBindingContext dbc) {
		TabItem serverTemplatesTab = new TabItem(tabFolder, SWT.NONE);
		serverTemplatesTab.setText("Server application source");

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

		IObservableValue eclipseProjectObservable = 
				BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_ECLIPSE_PROJECT).observe(model);
		DataBindingUtils.addDisposableValueChangeListener(new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				filterTemplates(txtTemplateFilter, (org.eclipse.core.resources.IProject) event.getObservableValue().getValue());
			}
		}, eclipseProjectObservable, txtTemplateFilter);
		
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
			.converting(new ObservableTreeItem2ModelConverter(IApplicationSource.class))
			.to(BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SERVER_APP_SOURCE).observe(model))
			.converting(new Model2ObservableTreeItemConverter(ApplicationSourceTreeItems.INSTANCE))
			.in(dbc);

		templatesViewer.addDoubleClickListener(onServerTemplateDoubleClicked());
		txtTemplateFilter.addModifyListener(onFilterTextTyped(templatesViewer));
		tabFolderTraverseListener.bindTabControls(
				tabFolder.getItemCount() - 1, txtTemplateFilter, templatesViewer.getTree());
		return selectedViewerServerTemplate;
	}

	protected String findMatchingTags(org.eclipse.core.resources.IProject project) {
		return new ProjectBuilderTypeDetector().findTemplateFilter(project);
	}

	private void createDetailsGroup(Composite parent, DataBindingContext dbc) {
		// details
		Group detailsGroup = new Group(parent, SWT.NONE);
		detailsGroup.setText("Details");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false).span(3, 1).hint(SWT.DEFAULT, 106)
			.applyTo(detailsGroup);
		GridLayoutFactory.fillDefaults()
			.margins(10, 6).spacing(2, 2) //TODO fix margins
			.applyTo(detailsGroup);

		Composite detailsContainer = new Composite(detailsGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.applyTo(detailsContainer);

		new ApplicationSourceDetailViews(
			BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE).observe(model), null, detailsContainer, dbc)
			.createControls();

		// detail resources button
		Button btnDetails = new Button(detailsGroup, SWT.NONE);
		btnDetails.setText("Defined Resources...");
		GridDataFactory.fillDefaults()
				.align(SWT.RIGHT, SWT.CENTER)
				.applyTo(btnDetails);

		IObservableValue selectedTemplate =
				BeanProperties.value(IApplicationSourceListPageModel.PROPERTY_SELECTED_APP_SOURCE).observe(model);
		ValueBindingBuilder
				.bind(WidgetProperties.visible().observe(btnDetails))
				.notUpdatingParticipant()
				.to(selectedTemplate)
				.converting(new Converter(Object.class, Boolean.class) {

					@Override
					public Object convert(Object fromObject) {
						return fromObject != null 
								&& ResourceKind.TEMPLATE.equals(((IApplicationSource)fromObject).getSource().getKind());
					}
					
				})
				.in(dbc);
		btnDetails.addSelectionListener(onDefinedResourcesClicked());
	}

	private IDoubleClickListener onServerTemplateDoubleClicked() {
		return new IDoubleClickListener() {
	        @Override
	        public void doubleClick(DoubleClickEvent event) {
	            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
	            if (hasApplicationSource(selection.getFirstElement())
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
						BeanProperties.list(IApplicationSourceListPageModel.PROPERTY_APP_SOURCES),
						BeanProperties.list(ObservableTreeItem.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider = new ObservableListTreeContentProvider(
				childrenProperty.listFactory(), null);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new ObservableTreeItemStyledCellLabelProvider());
		viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		viewer.setComparator(new ApplicationSourceComparator());
		viewer.addFilter(new AppSourceViewerFilter(templateFilterText));
		templateFilterText.addKeyListener(onFilterKeyPressed(viewer));
		viewer.setInput(model);

		return viewer;
	}

	private static class AppSourceViewerFilter extends ViewerFilter {

		private Text filterText;

		public AppSourceViewerFilter(Text filterText) {
			this.filterText = filterText;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!(element instanceof ObservableTreeItem)
					|| !(((ObservableTreeItem) element).getModel() instanceof IApplicationSource)) {
				return true;
			}
			IApplicationSource appSource = (IApplicationSource) ((ObservableTreeItem) element).getModel();
			return ResourceUtils.isMatching(filterText.getText(), appSource.getName(), appSource.getTags());
		}
	}

	private SelectionAdapter onFileSystemBrowseClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = createFileDialog(model.getLocalAppSourceFileName());
				String file = dialog.open();
				setLocalTemplate(file);
			}

			private FileDialog createFileDialog(String selectedFile) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setText("Select an OpenShift template");
				if(isFile(selectedFile)) {
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
				ITemplate template = (ITemplate) model.getSelectedAppSource().getSource();
				new ResourceSummaryDialog(getShell(),
						template.getObjects(),
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

	/**
	 * Navigates templates with arrows up/down while focus remains on the filter text.
	 * 
	 * @param viewer
	 * @return
	 */
	private KeyListener onFilterKeyPressed(final TreeViewer viewer) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
					TreeItem[] items = viewer.getTree().getItems();
					if (items == null 
							|| items.length == 0) {
						return;
					}
					TreeItem[] s = viewer.getTree().getSelection();
					TreeItem next = null;
					if (s == null							|| s.length == 0) {
						next = (e.keyCode == SWT.ARROW_DOWN)? items[0] : items[items.length - 1];
					} else {
						for (int i = 0; i < items.length && next == null; i++) {
							if (items[i] == s[0]) {
								if (e.keyCode == SWT.ARROW_DOWN) {
									if (i + 1 < items.length) {
										next = items[i + 1];
									}
								} else {
									if (i > 0) {
										next = items[i - 1];
									}
								}
							}
						}
					}
					if (next != null) {
						viewer.setSelection(new StructuredSelection(next.getData()), true);
					}
					e.doit = false;
				}
			}
		};
	}

	@Override
    protected JobChainBuilder getLoadResourcesJobBuilder(boolean[] closeAfter, boolean closeOnCancel) {
        JobChainBuilder builder = super.getLoadResourcesJobBuilder(closeAfter, closeOnCancel);
        builder.runWhenSuccessfullyDone(new UIJob("Expanding resource tree...") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                templatesViewer.expandAll();
                return Status.OK_STATUS;
            }
        });
        return builder;
    }

	public static class ApplicationSourceComparator extends ViewerComparator {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (hasApplicationSource(e1) && hasApplicationSource(e2)) {
				String t1 = ((IApplicationSource)((ObservableTreeItem)e1).getModel()).getName();
				String t2 = ((IApplicationSource)((ObservableTreeItem)e2).getModel()).getName();
				return t1.compareTo(t2);
			}
			return super.compare(viewer, e1, e2);
		}
	}

	private static boolean hasApplicationSource(Object item) {
		return item instanceof IApplicationSource 
				|| item instanceof ObservableTreeItem 
				&& ((ObservableTreeItem)item).getModel() instanceof IApplicationSource;
	}

	/**
	 * A validator that validates this page based on the choice to use a local
	 * or server template and the settings that are required therefore.
	 */
	private class TemplateListPageValidator extends MultiValidator {

		private IObservableValue useLocalTemplateObservable;
		private IObservableValue localTemplateFilenameObservable;
		private IObservableValue serverTemplateObservable;
		private IObservableValue selectedTemplateObservable;
		private IObservableValue projectNameObservable;

		private IObservableList mutableTargets = new WritableList<>();
		private Composite composite;
		
		public TemplateListPageValidator(IObservableValue useLocalTemplate, IObservableValue localTemplateFilename, 
				IObservableValue serverTemplate, IObservableValue selectedTemplate, IObservableValue projectNameObservable, Composite composite) {
			this.useLocalTemplateObservable = useLocalTemplate;
			useLocalTemplate.getValue();
			this.localTemplateFilenameObservable = localTemplateFilename;
			this.serverTemplateObservable = serverTemplate;
			this.selectedTemplateObservable = selectedTemplate;
			this.projectNameObservable = projectNameObservable;
			this.composite = composite;
		}

		@Override
		protected IStatus validate() {
			IStatus status = ValidationStatus.ok();
			mutableTargets.clear();
			
			projectNameObservable.getValue();
			Object useLocalTemplate = useLocalTemplateObservable.getValue();
			Object localTemplateFilename = localTemplateFilenameObservable.getValue();
			serverTemplateObservable.getValue();
			Object selectedTemplate = selectedTemplateObservable.getValue();

			if(status.getSeverity() < IStatus.ERROR) {
				if (Boolean.TRUE.equals(useLocalTemplate)) {
					String localTemplate = (String)localTemplateFilename;
					if (StringUtils.isNotBlank(localTemplate)) {
						if (!OpenshiftUIConstants.URL_VALIDATOR.isValid(localTemplate) && !isFile(localTemplate)) {
							status = ValidationStatus.error(NLS.bind("{0} is not a valid file.", localTemplate));
							mutableTargets.add(localTemplateFilenameObservable);
						}
					} else {
						status = ValidationStatus.cancel("Please select a local template file or URL.");
						mutableTargets.add(localTemplateFilenameObservable);
					}
				} else {
					if (selectedTemplate == null){
						status = ValidationStatus.cancel("Please select an image or template.");
						mutableTargets.add(serverTemplateObservable);
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

	class ProjectNameValidator extends MultiValidator {
		private IObservableValue projectNameObservable;
		private IObservableList mutableTargets = new WritableList<>();
		private Composite composite;

		public ProjectNameValidator(IObservableValue projectNameObservable, Composite composite) {
			this.projectNameObservable = projectNameObservable;
			this.composite = composite;
		}

		protected IStatus validate() {
			IStatus status = ValidationStatus.ok();
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
							status = ValidationStatus.warning(
									NLS.bind("A remote Git repository using the HTTP(S) protocol must be defined on project {0}", projectName));
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
			}
			composite.redraw();
			return status;
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

    @Override
    public void handleValueChange(ValueChangeEvent<? extends Object> event) {
        if (model.isUseLocalAppSource()) {
            if (StringUtils.isNotBlank(model.getLocalAppSourceFileName())) {
                Job job = new Job("Loading application source") {
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        model.loadAppSource(monitor);
                        return Status.OK_STATUS;
                    }
                };
                try {
                    WizardUtils.runInWizard(job, getContainer(), getDatabindingContext());
                } catch (InvocationTargetException | InterruptedException e) {
                    OpenShiftUIActivator.getDefault().getLogger().logError(e);
                }
            } else {
                model.resetLocalAppSource();
            }
        }
    }

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this,
				dbc);
	}

}
