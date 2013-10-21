/******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 *****************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;

import com.openshift.client.IApplication;
import com.openshift.client.IEnvironmentVariable;

/**
 * @author Martes G Wigglesworth
 * @author Martin Rieman <mrieman@redhat.com>
 * 
 */
public class ApplicationEnvironmentalVariableConfigurationWizardPage extends AbstractOpenShiftWizardPage {

	/**
	 * Constructs a new instance of
	 * ApplicationEnvironmentalVariableConfigurationWizardPage
	 * 
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 * @param iApplication 
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPage(String title, String description, String pageName,
			IWizard wizard, IApplication iApplication) {
		super(title, description, pageName, wizard);

		pageModel = new ApplicationEnvironmentalVariableConfigurationWizardPageModel(iApplication);

	}

	/**
	 * Constructs a new instance of
	 * ApplicationEnvironmentalVariableConfigurationWizardPage
	 * 
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPage(String title, String description, String pageName,
			IWizard wizard,DataBindingContext dbc) {
		super(title, description, pageName, wizard);
		//pageModel = wizardModel;

	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IEnvironmentVariable>() {

					@Override
					public String getValue(IEnvironmentVariable e) {
						return e.getName();
					}
				})
				.name("Variable Name").align(SWT.CENTER).weight(2).minWidth(100).buildColumn()
				.column(new IColumnLabelProvider<IEnvironmentVariable>() {

					@Override
					public String getValue(IEnvironmentVariable e) {
						return e.getValue();
					}
				})
				.name("Variable Value").align(SWT.CENTER).weight(2).minWidth(100).buildColumn()
				.buildViewer();

		return viewer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.openshift.express.internal.ui.wizard.
	 * AbstractOpenShiftWizardPage
	 * #doCreateControls(org.eclipse.swt.widgets.Composite,
	 * org.eclipse.core.databinding.DataBindingContext)
	 */
	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc)
	{

		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		Group envVariableGroup = new Group(container, SWT.NONE);
		//TODO - Add call to method to generate "on-demand" application name.
		envVariableGroup.setText("Environmental Variables for this Application");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(envVariableGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(envVariableGroup);

		Composite tableContainer = new Composite(envVariableGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
			.to(BeanProperties.value(ApplicationEnvironmentalVariableConfigurationWizardPageModel.PROPERTY_SELECTED_VARIABLE).observe(pageModel))
			.in(dbc);

		Button addButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add...");
		addButton.addSelectionListener(onAdd());

		Button editExistingButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText("Edit...");
		editExistingButton.addSelectionListener(onEditExisting());
		editExistingButton.setEnabled(pageModel.isPopulatedModel());

		Button removeButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.setEnabled(pageModel.isPopulatedModel());//This should be bound to the existence of variables in the table.
		removeButton.addSelectionListener(onRemove());
		/*
		Button importButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(importButton);
		importButton.setText("Import...");
		importButton.addSelectionListener(onImport());

		Button exportButton = new Button(envVariableGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(exportButton);
		exportButton.setText("Export...");
		exportButton.setEnabled(pageModel.isPopulatedModel());//This should be bound to the existence of variables in the table.
		exportButton.addSelectionListener(onExport());
		 */
		Composite filler = new Composite(envVariableGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		addButton.addSelectionListener(variablesListListerner(editExistingButton, removeButton/*, exportButton*/));
		editExistingButton.addSelectionListener(variablesListListerner(editExistingButton, removeButton/*, exportButton*/));
		removeButton.addSelectionListener(variablesListListerner(editExistingButton, removeButton/*, exportButton*/));
		/*importButton.addSelectionListener(variablesListListerner(editExistingButton, removeButton, exportButton));*/
		
		
		createAppVariableConfigurationGroup(container, dbc);
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			// Load current environmental variables into model.
		} catch (Exception e) {
			StatusManager.getManager().handle(
					OpenShiftUIActivator.createErrorStatus("Could not load Environmental Variables.", e),
					StatusManager.LOG);
		}
	}

	/*
	 * Create environmental variable configuration group
	 * 
	 * @author Martes G Wigglesworth
	 */
	private void createAppVariableConfigurationGroup(Composite parent, DataBindingContext dbc) {

	}

	/**
	 * Add Button Method
	 * 
	 * @return
	 */
	private SelectionListener onAdd() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ApplicationEnvironmentalVariablesAddWizard wizard = new ApplicationEnvironmentalVariablesAddWizard(pageModel);
				OkCancelButtonWizardDialog addVariableWizardDialog =
					new OkCancelButtonWizardDialog(getShell(), wizard);
				addVariableWizardDialog.open();
				refreshView();
			}
		};
	}
	
	/**
	 * Edit Button Method
	 * 
	 * @return
	 */
	private SelectionListener onEditExisting() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
					OkCancelButtonWizardDialog editVariableWizardDialog =
						new OkCancelButtonWizardDialog(getShell(), new ApplicationEnvironmentalVariableEditWizard(pageModel));
			editVariableWizardDialog.open();
			refreshView();
			}
		};
	}

	/**
	 * Import button method
	 * 
	 * @return
	 */
	private SelectionListener onImport() {
			return new SelectionAdapter() {

		};
	}

	/**
	 * Export Button Method
	 * 
	 * @return
	 */
	private SelectionListener onExport() {
		return new SelectionAdapter() {
		};
	}

	/**
	 * remove button Method
	 * 
	 * @return
	 */
	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				IEnvironmentVariable selectedVariable = pageModel.getSelectedVariable();
				if (MessageDialog.openConfirm(getShell(),
						"Remove Environment Variable",
						NLS.bind(
							"Are you sure that you want to remove the variable {0} from your OpenShift application?",
							selectedVariable.getName()+"="+selectedVariable.getValue())))
					try {
						pageModel.delete(selectedVariable);
						WizardUtils.runInWizard(
							new JobChainBuilder(new RemoveVarJob()).andRunWhenDone(
								new RefreshViewerJob()).build(), getContainer(), getDatabindingContext() );
					} catch (Exception ex) {
						StatusManager.getManager().handle(
							OpenShiftUIActivator.createErrorStatus(NLS.bind("Could not remove variable {0}.", selectedVariable), ex),
							StatusManager.LOG);
					}
			}
		};
	}
	
	/**
	 * private class used in the removal of an Environment Variable from the list
	 * 
	 * @author Martin Rieman <mrieman@redhat.com>
	 *
	 */
	private class RemoveVarJob extends Job {

		private RemoveVarJob() {
			super(NLS.bind("Removing Environment Variable {0}...", pageModel.getSelectedVariable()));
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			pageModel.removeVariable();
			return Status.OK_STATUS;
		}
	}
	
	/**
	 * Method to refresh the list of environment variables
	 */
	public void refreshView(){
		try {
			WizardUtils.runInWizard(new RefreshViewerJob(), getContainer(), getDatabindingContext() );
		} catch (Exception ex) {
			StatusManager.getManager().handle(
					OpenShiftUIActivator.createErrorStatus("Could not refresh variables.", ex), StatusManager.LOG);
		}
	}

	/**
	 * Private class used used to assist in refreshing the variables in the current window
	 * @author Martin Rieman <mrieman@redhat.com>
	 *
	 */
	private class RefreshViewerJob extends UIJob {

		public RefreshViewerJob() {
			super("Refreshing Environment Variables ...");
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			IEnvironmentVariable envVariable = pageModel.getSelectedVariable();
			viewer.setInput(pageModel.getVariablesDB());
			if (envVariable != null) {
				viewer.setSelection(new StructuredSelection(envVariable),true);
			}
			return Status.OK_STATUS;
		}
	}
	
	/**
	 * Listener to refresh the enabled/disabled buttons
	 * @param buttons
	 * @return
	 */
	private SelectionListener variablesListListerner(final Button... buttons) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Button button : buttons){
					button.setEnabled(pageModel.isPopulatedModel());
				}
			}
		};
	}
	
	private ApplicationEnvironmentalVariableConfigurationWizardPageModel pageModel;
	private TableViewer viewer;
}
