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
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.SSHKeysWizardPageModel;

import com.openshift.client.IOpenShiftSSHKey;

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

	protected TableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.viewer = new TableViewerBuilder(table, tableContainer)
				.contentProvider(new ArrayContentProvider())
				.column(new IColumnLabelProvider<IOpenShiftSSHKey>() {

					@Override
					public String getValue(IOpenShiftSSHKey key) {
						return key.getName();
					}
				})
				.name("Variable Name").align(SWT.LEFT).weight(2).minWidth(200).buildColumn()
				.column(new IColumnLabelProvider<IOpenShiftSSHKey>() {

					@Override
					public String getValue(IOpenShiftSSHKey key) {
						return key.getKeyType().getTypeId();
					}
				})
				.name("Value").align(SWT.LEFT).weight(4).minWidth(100).buildColumn()
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
		//setWizardPageDescription("Environmental Variables Configuration Wizard");
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(container);

		Group keysGroup = new Group(container, SWT.NONE);
		keysGroup.setText("Environmental Variables for this Application");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(keysGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(keysGroup);

		Composite tableContainer = new Composite(keysGroup, SWT.NONE);
		this.viewer = createTable(tableContainer);
		GridDataFactory.fillDefaults()
				.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(viewer))
				.to(BeanProperties.value(SSHKeysWizardPageModel.PROPERTY_SELECTED_KEY).observe(pageModel))
				.in(dbc);

		Button addExistingButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addExistingButton);
		addExistingButton.setText("Add...");
		addExistingButton.addSelectionListener(onAddExisting());

		Button addNewButton = new Button(keysGroup, SWT.PUSH);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(addNewButton);
		addNewButton.setText("Edit...");
		addNewButton.addSelectionListener(onAddNew());

		Button removeButton = new Button(keysGroup, SWT.PUSH);		
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(removeButton);
		removeButton.setText("Remove...");
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
		exportButton.addSelectionListener(onExport());
		
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.to(ViewerProperties.singleSelection().observe(viewer))
				.converting(new Converter(IOpenShiftSSHKey.class, Boolean.class) {

					@Override
					public Object convert(Object fromObject) {
						IOpenShiftSSHKey key = (IOpenShiftSSHKey) fromObject;
						return key != null;
					}
				})
				.in(dbc);

		Composite filler = new Composite(keysGroup, SWT.None);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(filler);

		createAppVariableConfigurationGroup(container, dbc);
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			// Load current environmental variables into model.
System.out.print("testing");
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

	private SelectionListener onAddExisting() {
		return new SelectionAdapter() {
			/*
			 * @Override public void widgetSelected(SelectionEvent e) {
			 * AddSSHKeyWizard wizard = new
			 * AddSSHKeyWizard(pageModel.getConnection()); if
			 * (WizardUtils.openWizardDialog(wizard, getShell()) ==
			 * Dialog.CANCEL) { return; }
			 * 
			 * try { WizardUtils.runInWizard( new RefreshViewerJob(),
			 * getContainer(), getDatabindingContext());
			 * pageModel.setSelectedSSHKey(wizard.getSSHKey()); } catch
			 * (Exception ex) { StatusManager.getManager().handle(
			 * OpenShiftUIActivator.createErrorStatus("Could not refresh keys.",
			 * ex), StatusManager.LOG); } }
			 */
		};
	}

	private SelectionListener onAddNew() {
		return new SelectionAdapter() {
			/*
			 * @Override public void widgetSelected(SelectionEvent e) {
			 * NewSSHKeyWizard wizard = new
			 * NewSSHKeyWizard(pageModel.getConnection()); if
			 * (WizardUtils.openWizardDialog(wizard, getShell()) ==
			 * Dialog.CANCEL) { return; }
			 * 
			 * try { WizardUtils.runInWizard( new RefreshViewerJob(),
			 * getContainer(), getDatabindingContext());
			 * pageModel.setSelectedSSHKey(wizard.getSSHKey()); } catch
			 * (Exception ex) { StatusManager.getManager().handle(
			 * OpenShiftUIActivator.createErrorStatus("Could not refresh keys.",
			 * ex), StatusManager.LOG); } }
			 */
		};
	}
	private SelectionListener onImport() {
		return new SelectionAdapter() {
			/*
			 * @Override public void widgetSelected(SelectionEvent e) {
			 * NewSSHKeyWizard wizard = new
			 * NewSSHKeyWizard(pageModel.getConnection()); if
			 * (WizardUtils.openWizardDialog(wizard, getShell()) ==
			 * Dialog.CANCEL) { return; }
			 * 
			 * try { WizardUtils.runInWizard( new RefreshViewerJob(),
			 * getContainer(), getDatabindingContext());
			 * pageModel.setSelectedSSHKey(wizard.getSSHKey()); } catch
			 * (Exception ex) { StatusManager.getManager().handle(
			 * OpenShiftUIActivator.createErrorStatus("Could not refresh keys.",
			 * ex), StatusManager.LOG); } }
			 */
		};
	}
	private SelectionListener onExport() {
		return new SelectionAdapter() {
			/*
			 * @Override public void widgetSelected(SelectionEvent e) {
			 * NewSSHKeyWizard wizard = new
			 * NewSSHKeyWizard(pageModel.getConnection()); if
			 * (WizardUtils.openWizardDialog(wizard, getShell()) ==
			 * Dialog.CANCEL) { return; }
			 * 
			 * try { WizardUtils.runInWizard( new RefreshViewerJob(),
			 * getContainer(), getDatabindingContext());
			 * pageModel.setSelectedSSHKey(wizard.getSSHKey()); } catch
			 * (Exception ex) { StatusManager.getManager().handle(
			 * OpenShiftUIActivator.createErrorStatus("Could not refresh keys.",
			 * ex), StatusManager.LOG); } }
			 */
		};
	}

	private SelectionListener onRefresh() {
		return new SelectionAdapter() {

		};
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {
			/*
			 * @Override public void widgetSelected(SelectionEvent e) { String
			 * keyName = pageModel.getSelectedSSHKey().getName(); if
			 * (MessageDialog.openConfirm(getShell(), "Remove SSH Key",
			 * NLS.bind(
			 * "Are you sure that you want to remove public SSH key {0} from OpenShift?"
			 * , keyName))) try { WizardUtils.runInWizard( new JobChainBuilder(
			 * new RemoveKeyJob()).andRunWhenDone(new
			 * RefreshViewerJob()).build() , getContainer(),
			 * getDatabindingContext() ); } catch (Exception ex) {
			 * StatusManager.getManager().handle(
			 * OpenShiftUIActivator.createErrorStatus
			 * (NLS.bind("Could not remove key {0}.", keyName), ex),
			 * StatusManager.LOG); } }
			 */
		};
	}

	private void setWizardPageDescription(String newDescription)
	{
		//pageModel.setDescription(newDescription);
	}

	private ApplicationEnvironmentalVariableConfigurationWizardPageModel pageModel;
	private TableViewer viewer;
}
