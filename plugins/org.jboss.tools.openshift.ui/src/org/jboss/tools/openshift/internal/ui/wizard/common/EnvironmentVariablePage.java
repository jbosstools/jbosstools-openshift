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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueWizardModel;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueItem;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizard;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizardModelBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.validator.EnvironmentVarKeyValidator;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.IDeploymentConfigPageModel;

/**
 * Base for adding env variable section to a page
 * @author jeff.cantrill
 *
 */
public abstract class EnvironmentVariablePage extends AbstractOpenShiftWizardPage{
	
	private static final String ENVIRONMENT_VARIABLE_LABEL = "Environment Variable";
	public static final String TABLE_LABEL = "Environment variables";
	private IEnvironmentVariablesPageModel model;
	private TableViewer envViewer;
	private Label lblEnvVars;

	protected int heightScale = 30;
	protected Composite envTableContainer;

	protected EnvironmentVariablePage(String title, String description, String name, IWizard wizard, IEnvironmentVariablesPageModel model) {
		super(title, description, name, wizard);
		this.model = model;
	}
	
	protected void setTableLabel(String value) {
		if(lblEnvVars == null) return;
		this.lblEnvVars.setText(value);
	}

	protected void setTableLabelToolToop(String value) {
		if(lblEnvVars == null) return;
		this.lblEnvVars.setToolTipText(value);
	}
	
	@SuppressWarnings("unchecked")
	protected void createEnvVariableControl(Composite parent, DataBindingContext dbc) {
		Composite envContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(envContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(2, 2).applyTo(envContainer);
		
		Label lblEnvVars = new Label(envContainer, SWT.NONE);
		lblEnvVars.setText("Environment variables:");
		lblEnvVars.setToolTipText("Environment variables are passed to running pods for consumption by the pod containers");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,1)
			.applyTo(lblEnvVars);
		Composite tableContainer = envTableContainer = new Composite(envContainer, SWT.NONE);
		
		this.envViewer = createEnvVarTable(tableContainer);
		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 150).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(envViewer))
				.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE)
				.observe(model))
				.in(dbc);
		envViewer.setContentProvider(new ObservableListContentProvider());
		envViewer.setInput(BeanProperties.list(
				IDeploymentConfigPageModel.PROPERTY_ENVIRONMENT_VARIABLES).observe(model));
		
		Button addButton = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING).applyTo(addButton);
		addButton.setText("Add...");
		addButton.setToolTipText("Add an environment variable declared by the docker image.");
		addButton.addSelectionListener(onAdd());
		UIUtils.setDefaultButtonWidth(addButton);
		heightScale = addButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		Button editExistingButton = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING).applyTo(editExistingButton);
		editExistingButton.setText("Edit...");
		editExistingButton.setToolTipText("Edit the environment variable declared by the docker image.");
		editExistingButton.addSelectionListener(new EditHandler());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editExistingButton))
				.notUpdatingParticipant()
				.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE).observe(model))
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		UIUtils.setDefaultButtonWidth(editExistingButton);
		
		Button btnReset = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING).applyTo(btnReset);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Reset to the value declared by the docker image.");
		btnReset.addSelectionListener(onResetEnvVar());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnReset))
					.notUpdatingParticipant()
					.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE).observe(model))
				.converting(new IsNotNull2BooleanConverter() {

					@Override
					public Object convert(Object fromObject) {
						Boolean notNull = (Boolean) super.convert(fromObject);
						return notNull && !((EnvironmentVariable)fromObject).isNew() && model.isEnvironmentVariableModified((EnvironmentVariable)fromObject);
					}
					
				})
				.in(dbc);
		UIUtils.setDefaultButtonWidth(btnReset);

		Button btnResetAll = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.BEGINNING).applyTo(btnResetAll);
		btnResetAll.setText("Reset All");
		btnResetAll.setToolTipText("Reset all variables to the value declared by the docker image.");
		btnResetAll.addSelectionListener(onResetEnvVars());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(btnResetAll))
					.notUpdatingParticipant()
					//This may look like a hack, but this property do change at each change to variables, so that button refresh will be consistent. 
					.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE).observe(model))
				.converting(new IsNotNull2BooleanConverter() {

					@Override
					public Object convert(Object fromObject) {
						List<EnvironmentVariable> vars = model.getEnvironmentVariables();
						return vars != null && !vars.isEmpty() && vars.stream().anyMatch(v -> !v.isNew() && model.isEnvironmentVariableModified(v));
					}

				})
				.in(dbc);
		UIUtils.setDefaultButtonWidth(btnResetAll);

		Button btnRemove = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.BEGINNING).applyTo(btnRemove);
		btnRemove.setText("Remove");
		btnRemove.setToolTipText("Remove the environment variable added here.");
		btnRemove.addSelectionListener(onRemoveEnvVar());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(btnRemove))
			.notUpdatingParticipant()
			.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE).observe(model))
			.converting(new IsNotNull2BooleanConverter() {
				
				@Override
				public Object convert(Object fromObject) {
					Boolean notNull = (Boolean) super.convert(fromObject);
					return notNull && ((EnvironmentVariable)fromObject).isNew();
				}
				
			})
			.in(dbc);
		UIUtils.setDefaultButtonWidth(btnRemove);
	}
	
	private SelectionListener onResetEnvVar() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				EnvironmentVariable envVar = UIUtils.getFirstElement(envViewer.getSelection(), EnvironmentVariable.class);
				if(MessageDialog.openQuestion(getShell(), "Reset " + ENVIRONMENT_VARIABLE_LABEL, 
						NLS.bind("Are you sure you want to reset the {0} {1}?", ENVIRONMENT_VARIABLE_LABEL.toLowerCase(), envVar.getKey()))) {
					String key = envVar.getKey();
					model.resetEnvironmentVariable(envVar);
					selectEnvVarByKey(key);
				}
			}

		};
	}

	private SelectionListener onResetEnvVars() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				List<EnvironmentVariable> vars = model.getEnvironmentVariables();
				EnvironmentVariable envVar = UIUtils.getFirstElement(envViewer.getSelection(), EnvironmentVariable.class);
				if(MessageDialog.openQuestion(getShell(), "Reset " + ENVIRONMENT_VARIABLE_LABEL, 
						NLS.bind("Are you sure you want to reset all {0}s?", ENVIRONMENT_VARIABLE_LABEL.toLowerCase()))) {
					vars.stream().forEach(v -> model.resetEnvironmentVariable(v));
					if(envVar != null) {
						selectEnvVarByKey(envVar.getKey());
					}
				}
			}
			
		};
	}

	private SelectionListener onRemoveEnvVar() {
		return new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				EnvironmentVariable envVar = UIUtils.getFirstElement(envViewer.getSelection(), EnvironmentVariable.class);
				if(MessageDialog.openQuestion(getShell(), "Remove " + ENVIRONMENT_VARIABLE_LABEL, 
						NLS.bind("Are you sure you want to remove the {0} {1}?", ENVIRONMENT_VARIABLE_LABEL.toLowerCase(), envVar.getKey()))) {
					model.removeEnvironmentVariable(envVar);
					envViewer.refresh();
				}
			}
			
		};
	}

	private class EditHandler extends SelectionAdapter implements IDoubleClickListener {
		@Override
		public void widgetSelected(SelectionEvent e) {
			handleEvent();
		}

		@Override
		public void doubleClick(DoubleClickEvent event) {
			handleEvent();
		}
		
		private void handleEvent() {
			EnvironmentVariable var = UIUtils.getFirstElement(envViewer.getSelection(), EnvironmentVariable.class);
			Set<String> usedKeys = model.getEnvironmentVariables().stream().map(v -> v.getKey()).collect(Collectors.toSet());
			usedKeys.remove(var.getKey());
			IKeyValueWizardModel<IKeyValueItem> dialogModel = new KeyValueWizardModelBuilder<IKeyValueItem>(var)
					.windowTitle(ENVIRONMENT_VARIABLE_LABEL)
					.title("Edit " + ENVIRONMENT_VARIABLE_LABEL)
					.description(NLS.bind("Edit the {0}.", ENVIRONMENT_VARIABLE_LABEL.toLowerCase()))
					.keyLabel(ENVIRONMENT_VARIABLE_LABEL)
					.editableKey(var.isNew())
					.groupLabel(ENVIRONMENT_VARIABLE_LABEL)
					.keyAfterConvertValidator(new EnvironmentVarKeyValidator(usedKeys))
					.build();
			OkCancelButtonWizardDialog dialog =
					new OkCancelButtonWizardDialog(getShell(),
							new KeyValueWizard<>(var, dialogModel));
			if(OkCancelButtonWizardDialog.OK == dialog.open()) {
				model.updateEnvironmentVariable(var, dialogModel.getKey(), dialogModel.getValue());
				model.resetEnvironmentVariable(var);
				selectEnvVarByKey(dialogModel.getKey());
			}
		}
		
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Set<String> usedKeys = model.getEnvironmentVariables().stream().map(v -> v.getKey()).collect(Collectors.toSet());
				IKeyValueWizardModel<KeyValueItem> dialogModel = new KeyValueWizardModelBuilder<KeyValueItem>()
						.windowTitle(ENVIRONMENT_VARIABLE_LABEL)
						.title("Add " + ENVIRONMENT_VARIABLE_LABEL)
						.description(NLS.bind("Add an {0}.", ENVIRONMENT_VARIABLE_LABEL.toLowerCase()))
						.keyLabel("Name")
						.groupLabel(ENVIRONMENT_VARIABLE_LABEL)
						.keyAfterConvertValidator(new EnvironmentVarKeyValidator(usedKeys))
						.build();
				OkCancelButtonWizardDialog dialog =
						new OkCancelButtonWizardDialog(getShell(),
								new KeyValueWizard<>(dialogModel));
				if(OkCancelButtonWizardDialog.OK == dialog.open()) {
					model.addEnvironmentVariable(dialogModel.getKey(), dialogModel.getValue());
					selectEnvVarByKey(dialogModel.getKey());
				}
			}
		};
	}

	private void selectEnvVarByKey(String key) {
		EnvironmentVariable envVar = model.getEnvironmentVariable(key);
		if(envVar != null) {
			envViewer.setSelection(new StructuredSelection(envVar));
			envViewer.reveal(envVar);
		}
	}

	protected TableViewer createEnvVarTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.envViewer = new TableViewerBuilder(table, tableContainer)
				.column(new IColumnLabelProvider<IKeyValueItem>() {
					@Override
					public String getValue(IKeyValueItem label) {
						return label.getKey();
					}
					@Override
					public boolean isModified(IKeyValueItem e) {
						return ((EnvironmentVariable)e).isNew();
					}
				})
				.name("Name").align(SWT.LEFT).weight(2).minWidth(75).buildColumn()
				.column(new IColumnLabelProvider<IKeyValueItem>() {
					@Override
					public String getValue(IKeyValueItem label) {
						return label.getValue();
					}
					@Override
					public boolean isModified(IKeyValueItem e) {
						return model.isEnvironmentVariableModified((EnvironmentVariable)e);
					}
				})
				.name("Value").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();
		envViewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				IKeyValueItem first = (IKeyValueItem) e1;
				IKeyValueItem other = (IKeyValueItem) e2;
				return first.getKey().compareTo(other.getKey());
			}
			
		});
		envViewer.addDoubleClickListener(new EditHandler());
		return envViewer;
	}

}
