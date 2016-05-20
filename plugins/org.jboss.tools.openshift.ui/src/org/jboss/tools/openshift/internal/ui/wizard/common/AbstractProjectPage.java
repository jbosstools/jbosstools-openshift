/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.common;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.job.UIUpdatingJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.comparators.ProjectViewerComparator;
import org.jboss.tools.openshift.internal.ui.explorer.OpenShiftExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.treeitem.Model2ObservableTreeItemConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItem2ModelConverter;
import org.jboss.tools.openshift.internal.ui.treeitem.ObservableTreeItemLabelProvider;
import org.jboss.tools.openshift.internal.ui.wizard.project.NewProjectWizard;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IProject;

/**
 * A page that offers a list templates to a user
 *
 * @author jeff.cantrill
 * @author Andre Dietisheim
 * @author Jeff Maury
 * @param <C> the page model
 *
 */
public class AbstractProjectPage<M extends IProjectPageModel> extends AbstractOpenShiftWizardPage  {

    protected M model;

	public AbstractProjectPage(IWizard wizard, M model, String title, String description,
	                           String pageName) {
		super(title, 
			  description, 
			  pageName, 
			  wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(10, 6).spacing(6, 6)
			.applyTo(parent);

		createProjectControls(parent, dbc);
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

		final OpenShiftExplorerLabelProvider labelProvider = new OpenShiftExplorerLabelProvider();
		projectsViewer.setContentProvider(new ObservableListContentProvider());
		projectsViewer.setLabelProvider(new ObservableTreeItemLabelProvider());
		projectsViewer.setInput(
				BeanProperties.list(IProjectPageModel.PROPERTY_PROJECT_ITEMS).observe(model));
		projectsViewer.setComparator(ProjectViewerComparator.createProjectTreeSorter(labelProvider));

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
					.to(BeanProperties.value(IProjectPageModel.PROPERTY_PROJECT)
					.observe(model))
					.converting(new Model2ObservableTreeItemConverter(null))
					.in(dbc);
		ControlDecorationSupport.create(
				selectedProjectBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		IObservableValue connectionObservable = BeanProperties.value(IProjectPageModel.PROPERTY_CONNECTION).observe(model);
		DataBindingUtils.addDisposableValueChangeListener(
				onConnectionChanged(), connectionObservable, projectsViewer.getControl());

		Button newProjectButton = new Button(parent, SWT.PUSH);
		newProjectButton.setText("New...");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER)
			.applyTo(newProjectButton);
		UIUtils.setDefaultButtonWidth(newProjectButton);
		newProjectButton.addSelectionListener(onNewProjectClicked());

		Label filler = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).span(3, 1)
			
			.applyTo(filler);
	}
	
    private IValueChangeListener onConnectionChanged() {
        return new IValueChangeListener() {

            @Override
            public void handleValueChange(ValueChangeEvent event) {
                loadResources(getPreviousPage() == null);
            }
        };
    }

    private SelectionAdapter onNewProjectClicked() {
        return new SelectionAdapter() {

            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    // run in job to enforce busy cursor which doesnt work otherwise
                    WizardUtils.runInWizard(new UIUpdatingJob("Opening projects wizard...") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            return Status.OK_STATUS;
                        }

                        @Override
                        protected IStatus updateUI(IProgressMonitor monitor) {
                            NewProjectWizard newProjectWizard = new NewProjectWizard(
                                    (Connection) model.getConnection(), model.getProjectItems().stream().map(it->(IProject)it.getModel()).collect(Collectors.toList()));
                            int result = new OkCancelButtonWizardDialog(getShell(), newProjectWizard).open();
                            // reload projects to reflect changes that happened in
                            // projects wizard
                            if (newProjectWizard.getProject() != null) {
                                loadResources(false);
                            }
                            if (Dialog.OK == result) {
                                IProject selectedProject = newProjectWizard.getProject();
                                if (selectedProject != null) {
                                    model.setProject(selectedProject);
                                }
                            }
                            ;
                            return Status.OK_STATUS;
                        }

                    }, getContainer());
                } catch (InvocationTargetException | InterruptedException ex) {
                    // swallow intentionnally
                }
            }
            
        };
    }
    
    /**
     * Create and configure the list of jobs that need to be performed during resource loading.
     * The base behavior is to load the projects and force project creation if no project exists.
     * 
     * @param closeAfter return parameter if wizard needs to be closed (may be updated)
     * @param closeOnCancel true if the wizard need to be closed
     * @return the job builder
     */
    protected JobChainBuilder getLoadResourcesJobBuilder(final boolean closeAfter[], final boolean closeOnCancel) {
        JobChainBuilder builder = new JobChainBuilder(
                new AbstractDelegatingMonitorJob("Loading projects...") {

                    @Override
                    protected IStatus doRun(IProgressMonitor monitor) {
                        try {
                            model.loadResources();
                        } catch (OpenShiftException e) {
                            closeAfter[0] = closeOnCancel;
                            String problem = e.getStatus() == null ? e.getMessage() : e.getStatus().getMessage();
                            return  OpenShiftUIActivator.statusFactory().errorStatus(problem, e);
                        }
                        return Status.OK_STATUS;
                    }
                });
        builder.runWhenSuccessfullyDone(new UIJob("Verifying required project...") {

                    @Override
                    public IStatus runInUIThread(IProgressMonitor monitor) {
                        if(!model.hasProjects()) {
                            List<IProject> projects = new ObservableTreeItem2ModelConverter().convert(model.getProjectItems());
                            Connection connection = (Connection) model.getConnection();
                            NewProjectWizard newProjectWizard = new NewProjectWizard(connection, projects);
                            if (Dialog.CANCEL ==
                                    WizardUtils.openWizardDialog(newProjectWizard, getShell())) {
                                closeAfter[0] = closeOnCancel;
                                return Status.CANCEL_STATUS;
                            } else {
                                model.loadResources();
                                model.setProject(newProjectWizard.getProject());
                            }
                        }
                        return Status.OK_STATUS;
                    }
                });
        return builder;
    }

	protected void loadResources(final boolean closeOnCancel) {
		if (!model.hasConnection()) {
			return;
		}
		try {
			final boolean[] closeAfter = new boolean[]{false}; 
			Job jobs = getLoadResourcesJobBuilder(closeAfter, closeOnCancel).build();
			WizardUtils.runInWizard(jobs, getContainer(), getDataBindingContext());
			if(closeAfter[0]) {
				if(Display.getCurrent() != null) {
					WizardUtils.close(getWizard());
				} else {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						WizardUtils.close(getWizard());
					}
				});
				}
			}
		} catch (InvocationTargetException | InterruptedException e) {
			// intentionnally swallowed
		}
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		loadResources(getPreviousPage() == null);
		// fix GTK3 combo boxes too small
		// https://issues.jboss.org/browse/JBIDE-16877,
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=431425
		((Composite) getControl()).layout(true, true);
	}


	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this,
				dbc);
	}
	
    protected static boolean isFile(String path) {
        try {
            return StringUtils.isNotBlank(path) && Files.isRegularFile(Paths.get(VariablesHelper.replaceVariables(path)));
        } catch (InvalidPathException e) {
            return false;
        }
    }
    
    protected static boolean exists(String path) {
        return StringUtils.isNotBlank(path) && Files.exists(Paths.get(VariablesHelper.replaceVariables(path)));
    }
}
