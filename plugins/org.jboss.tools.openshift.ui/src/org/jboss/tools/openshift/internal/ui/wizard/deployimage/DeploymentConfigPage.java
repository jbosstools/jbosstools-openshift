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
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;


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
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueItem;
import org.jboss.tools.openshift.internal.common.ui.wizard.IKeyValueWizardModel;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueItem;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizard;
import org.jboss.tools.openshift.internal.common.ui.wizard.KeyValueWizardModelBuilder;
import org.jboss.tools.openshift.internal.common.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.internal.ui.validator.EnvironmentVarKeyValidator;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;

/**
 * Page to (mostly) edit the config items for a page
 * 
 * @author jeff.cantrill
 */
public class DeploymentConfigPage extends AbstractOpenShiftWizardPage {

	private static final String PAGE_NAME = "Deployment Config Settings Page";
	private static final String PAGE_TITLE = "Deployment Configuration && Scalability";
	private static final String PAGE_DESCRIPTION = "";
	private static final String ENVIRONMENT_VARIABLE_LABEL = "Environment Variable";

	private IDeploymentConfigPageModel model;
	private TableViewer envViewer;
	private TableViewer dataViewer;

	protected DeploymentConfigPage(IWizard wizard, IDeploymentConfigPageModel model) {
		super(PAGE_TITLE, PAGE_DESCRIPTION, PAGE_NAME, wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		//Env Variables Block
		createEnvVariableControl(parent, dbc);

		Label separator1 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator1);

		createDataVolumeControl(parent, dbc);
		
		Label separator2 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(separator2);

		//Scaling
		Composite scalingContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(scalingContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(scalingContainer);
		
		Label lblReplicas = new Label(scalingContainer, SWT.NONE);
		lblReplicas.setText("Replicas:");
		lblReplicas.setToolTipText("Replicas are the number of copies of an image that will be scheduled to run on OpenShift");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
			.applyTo(lblReplicas);
		
		Spinner replicas = new Spinner(scalingContainer, SWT.BORDER);
		replicas.setMinimum(1);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.applyTo(replicas);
		ValueBindingBuilder.bind(WidgetProperties.selection().observe(replicas))
			.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_REPLICAS)
			.observe(model))
			.in(dbc);
	}
	
	
	private void createEnvVariableControl(Composite parent, DataBindingContext dbc) {
		Composite envContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(envContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(envContainer);
		
		Label lblEnvVars = new Label(envContainer, SWT.NONE);
		lblEnvVars.setText("Environment variables:");
		lblEnvVars.setToolTipText("Environment variables are passed to running pods for consumption by the pod containers");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,1)
			.applyTo(lblEnvVars);
		Composite tableContainer = new Composite(envContainer, SWT.NONE);
		
		this.envViewer = createEnvVarTable(tableContainer);
		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(envViewer))
				.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_ENVIRONMENT_VARIABLE)
				.observe(model))
				.in(dbc);
		envViewer.setContentProvider(new ObservableListContentProvider());
		envViewer.setInput(BeanProperties.list(
				IDeploymentConfigPageModel.PROPERTY_ENVIRONMENT_VARIABLES).observe(model));
		
		Button addButton = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(addButton);
		addButton.setText("Add...");
		addButton.setToolTipText("Add an environment variable declared by the docker image.");
		addButton.addSelectionListener(onAdd());
		UIUtils.setDefaultButtonWidth(addButton);

		
		Button editExistingButton = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).applyTo(editExistingButton);
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
			.align(SWT.FILL, SWT.FILL).applyTo(btnReset);
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
						return notNull && !((EnvironmentVariable)fromObject).isNew();
					}
					
				})
				.in(dbc);
		UIUtils.setDefaultButtonWidth(btnReset);

		Button btnRemove = new Button(envContainer, SWT.PUSH);
		GridDataFactory.fillDefaults()
		.align(SWT.FILL, SWT.FILL).applyTo(btnRemove);
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

	private void createDataVolumeControl(Composite parent, DataBindingContext dbc) {
		Composite sectionContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(sectionContainer);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(6, 6).applyTo(sectionContainer);
		
		Label lblSection = new Label(sectionContainer, SWT.NONE);
		lblSection.setText("Data volumes:");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,1)
			.applyTo(lblSection);
		Composite tableContainer = new Composite(sectionContainer, SWT.NONE);
		
		dataViewer = createDataVolumeTable(tableContainer);
		dataViewer.setContentProvider(new ObservableListContentProvider());
		GridDataFactory.fillDefaults()
			.span(1, 5).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableContainer);
		ValueBindingBuilder.bind(ViewerProperties.singleSelection().observe(dataViewer))
			.to(BeanProperties.value(IDeploymentConfigPageModel.PROPERTY_SELECTED_VOLUME)
			.observe(model));
		dataViewer.setInput(BeanProperties.list(
				IDeploymentConfigPageModel.PROPERTY_VOLUMES).observe(model));
		
		Label lblNotice = new Label(sectionContainer, SWT.WRAP);
		lblNotice.setText(NLS.bind("NOTICE: This image might use an EmptyDir volume. Data in EmptyDir volumes is not persisted across deployments.", model.getResourceName()));
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL)
			.span(2,2)
			.applyTo(lblNotice);
	}
	
	private SelectionListener onResetEnvVar() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				EnvironmentVariable envVar = UIUtils.getFirstElement(envViewer.getSelection(), EnvironmentVariable.class);
				if(MessageDialog.openQuestion(getShell(), "Reset " + ENVIRONMENT_VARIABLE_LABEL, 
						NLS.bind("Are you sure you want to reset the {0} {1}?", ENVIRONMENT_VARIABLE_LABEL.toLowerCase(), envVar.getKey()))) {
					model.resetEnvironmentVariable(envVar);
					envViewer.refresh();
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
				}
			}
		};
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
				})
				.name("Name").align(SWT.LEFT).weight(2).minWidth(75).buildColumn()
				.column(new IColumnLabelProvider<IKeyValueItem>() {

					@Override
					public String getValue(IKeyValueItem label) {
						return label.getValue();
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

	protected TableViewer createDataVolumeTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		this.dataViewer = new TableViewerBuilder(table, tableContainer)
				.column(new IColumnLabelProvider<String>() {
					@Override
					public String getValue(String label) {
						return label;
					}
				})
				.name("Container Path").align(SWT.LEFT).weight(2).minWidth(100).buildColumn()
				.buildViewer();
		dataViewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				String first = (String) e1;
				String other = (String) e2;
				return first.compareTo(other);
			}
			
		});
		return dataViewer;
	}
	
}
