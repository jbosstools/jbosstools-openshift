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
package org.jboss.tools.openshift.internal.ui.wizard.project;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.common.ui.DelegatingProgressMonitor;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.core.plugin.log.IPluginLog;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.ICellToolTipProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.job.DeleteResourceJob;
import org.jboss.tools.openshift.internal.ui.job.OpenShiftJobs;

import com.openshift.restclient.model.IProject;

/**
 * @author jeff.cantrill
 */
public class ManageProjectsWizardPage extends AbstractOpenShiftWizardPage {

	private static final IPluginLog LOG = OpenShiftUIActivator.getDefault().getLogger();
	private ManageProjectsWizardPageModel pageModel;
	private TableViewer viewer;

	private List<IProject> initialProjects;

	public ManageProjectsWizardPage(String title, String description, ManageProjectsWizardPageModel pageModel, IWizard wizard) {
		super(title, description, title, wizard);
		this.pageModel = pageModel;
	}

	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group group = new Group(parent, SWT.NONE);
		group.setText("OpenShift Projects");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(group);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(group);

		// table
		Composite tableContainer = new Composite(group, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		viewer.setContentProvider(new ObservableListContentProvider());
		viewer.setInput(BeanProperties.list(
				ManageProjectsWizardPageModel.PROPERTY_PROJECTS).observe(pageModel));
		loadProjects(dbc);
		initialProjects = getProjects();

		IObservableValue viewerSingleSelection = ViewerProperties.singleSelection().observe(viewer);
		ValueBindingBuilder.bind(viewerSingleSelection)
				.to(BeanProperties.value(ManageProjectsWizardPageModel.PROPERTY_SELECTED_PROJECT).observe(pageModel))
				.in(dbc);

		// new 
		Button newButton = new Button(group, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(newButton);
		newButton.setText("New...");
		newButton.addSelectionListener(onNew());

		// remove
		Button removeButton = new Button(group, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.addSelectionListener(onRemove(dbc));
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.notUpdatingParticipant()
				.to(viewerSingleSelection)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);

		Composite filler = new Composite(group, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		// refresh
		Button refreshButton = new Button(group, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.END).applyTo(refreshButton);
		refreshButton.setText("Refresh...");
		refreshButton.addSelectionListener(onRefresh(dbc));
	}

	private void loadProjects(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob("Loading OpenShift projects...") {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.loadProjects();
							return Status.OK_STATUS;
						}
					}, new DelegatingProgressMonitor(), getContainer(), dbc);
		} catch (InvocationTargetException | InterruptedException e) {
			LOG.logError(NLS.bind("Could not load projects for connection {0}", pageModel.getConnection().toString()), e);
		}
	}

	private SelectionListener onNew() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				NewProjectWizard newProjectWizard = new NewProjectWizard(pageModel.getConnection(), pageModel.getProjects());
				int res = WizardUtils.openWizardDialog(newProjectWizard, getShell());
				if (res == IDialogConstants.OK_ID) {
					IProject newOrSelectedProject = newProjectWizard.getProject();
					if (newOrSelectedProject != null) {
						pageModel.setSelectedProject(newOrSelectedProject);
					}
				}
			}
		};
	}

	private SelectionListener onRemove(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IProject project = pageModel.getSelectedProject();
				if (project == null) {
					return;
				}
				boolean confirm = MessageDialog.openConfirm(getShell(), 
						OpenShiftUIMessages.ProjectDeletionDialogTitle, 
						NLS.bind(OpenShiftUIMessages.ProjectDeletionConfirmation, project.getName()));
				if (!confirm) {
					return;
				}
				DeleteResourceJob job = OpenShiftJobs.createDeleteProjectJob(project);
				try {
					org.jboss.tools.common.ui.WizardUtils.runInWizard(
							job, job.getDelegatingProgressMonitor(), getContainer(), dbc);
				} catch (InvocationTargetException | InterruptedException ex) {
					OpenShiftUIActivator.getDefault().getLogger().logError(NLS.bind("Could not delete OpenShift project {0}", project.getName()), ex);
				}
			}
		};
	}

	private SelectionListener onRefresh(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshModel(dbc);
			}
		};
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		ICellToolTipProvider<IProject> cellToolTipProvider = new ICellToolTipProvider<IProject>() {

			@Override
			public String getToolTipText(IProject object) {
				return object.getDescription();
			}

			@Override
			public int getToolTipDisplayDelayTime(IProject object) {
				return 0;
			}
		};
		
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IProject>() {

					@Override
					public String getValue(IProject project) {
						return project.getName();
					}
				})
				.cellToolTipProvider(cellToolTipProvider)
				.name("Name").align(SWT.LEFT).weight(1).minWidth(75).buildColumn()
				.column(new IColumnLabelProvider<IProject>() {

					@Override
					public String getValue(IProject project) {
						return project.getDisplayName();
					}
				})
				.cellToolTipProvider(cellToolTipProvider)
				.name("Display Name").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();

		return viewer;
	}

	private void refreshModel(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(
					new AbstractDelegatingMonitorJob("Refreshing Projects...") {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.refresh();
							return Status.OK_STATUS;
						}
					}
					, new DelegatingProgressMonitor(), getContainer(), dbc);
		} catch (InvocationTargetException | InterruptedException e) {
			LOG.logError(NLS.bind("Could not refresh projects for connection {0}", pageModel.getConnection().toString()), e);
		}
	}

	public IProject getSelectedProject() {
		return pageModel.getSelectedProject();
	}

	public List<IProject> getProjects() {
		return pageModel.getProjects();
	}

	@Override
	public void dispose() {
		pageModel.dispose();
		super.dispose();
	}

	public boolean hasChanged() {
		return !Objects.deepEquals(initialProjects,getProjects());
	}
}
