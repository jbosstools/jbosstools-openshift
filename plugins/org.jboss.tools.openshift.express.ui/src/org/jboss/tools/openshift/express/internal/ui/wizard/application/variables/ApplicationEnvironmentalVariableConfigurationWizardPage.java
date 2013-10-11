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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;

/**
 * @author Martes G Wigglesworth
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
	 */
	public ApplicationEnvironmentalVariableConfigurationWizardPage(String title, String description, String pageName,
			IWizard wizard) {
		super(title, description, pageName, wizard);
		// TODO Auto-generated constructor stub
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
		//pageModel.setVariableDB(dbc.)
	}

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<Object>() {

					/*
					 * Placeholder for environmental variable information
					 */
					@Override
					public String getValue(Object e) {
						// TODO Auto-generated method stub
						return null;
					}
				})
				.name("Variable Name").align(SWT.CENTER).weight(2).minWidth(100).buildColumn()
				.column(new IColumnLabelProvider<Object>() {

					/*
					 * Placeholder for environmental variable information
					 */
					@Override
					public String getValue(Object e) {
						// TODO Auto-generated method stub
						return null;
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

		Group keysGroup = new Group(container, SWT.NONE);
		//TODO - Add call to method to generate "on-demand" application name.
		keysGroup.setText("Environmental Variables for this Application");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(keysGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(keysGroup);

		Composite tableContainer = new Composite(keysGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);

		Button addButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add...");
		addButton.addSelectionListener(onAdd());

		Button editExistingButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
		editExistingButton.setText("Edit...");
		editExistingButton.addSelectionListener(onEditExisting());
		editExistingButton.setEnabled(pageModel.isEmpty());

		Button removeButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
		removeButton.setEnabled(pageModel.isEmpty());//This should be bound to the existence of variables in the table.
		removeButton.addSelectionListener(onRemove());

		Button importButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(importButton);
		importButton.setText("Import...");
		importButton.addSelectionListener(onImport());

		Button exportButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(exportButton);
		exportButton.setText("Export...");
		exportButton.setEnabled(pageModel.isEmpty());//This should be bound to the existence of variables in the table.
		exportButton.addSelectionListener(onExport());

		Composite filler = new Composite(keysGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

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

	private SelectionListener onAdd() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
					OkCancelButtonWizardDialog addVariableWizardDialog =
						new OkCancelButtonWizardDialog(getShell(), new ApplicationEnvironmentalVariablesAddWizard());
				addVariableWizardDialog.open();
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
			}
		};
	}

	
	private SelectionListener onImport() {
			return new SelectionAdapter() {

		};
	}

	private SelectionListener onExport() {
		return new SelectionAdapter() {
		};
	}

	

	private SelectionListener onRemove() {
		return new SelectionAdapter() {
			
		};
	}
	


	private ApplicationEnvironmentalVariableConfigurationWizardPageModel pageModel;
	private TableViewer viewer;
}
