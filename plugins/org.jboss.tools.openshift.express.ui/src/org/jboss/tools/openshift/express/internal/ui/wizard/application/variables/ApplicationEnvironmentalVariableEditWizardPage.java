/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;


/**
 * @author Martes G Wigglesworth
 * @author Martin Rieman <mrieman@redhat.com>
 *
 */
public class ApplicationEnvironmentalVariableEditWizardPage extends AbstractOpenShiftWizardPage {

	private ApplicationEnvironmentalVariableEditWizardPageModel pageModel;

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableConfigurationWizardPage
	 * @param variableName
	 * @param variableValue
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariableEditWizardPage(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel, IWizard wizard) {
		super(ApplicationEnvironmentalVariableEditWizardPageModel.PAGE_TITLE, 
			ApplicationEnvironmentalVariableEditWizardPageModel.PAGE_DESCRIPTION,
			ApplicationEnvironmentalVariableEditWizardPageModel.PAGE_NAME, wizard);
		pageModel = new ApplicationEnvironmentalVariableEditWizardPageModel(confPageModel);
	}

	/** 
	 * Creates the UI for ApplicationEnvironmentalVariableConfigurationWizardPage
	 * @param parent
	 * @param dbc
	 */
	/* (non-Javadoc)
	 * @see org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage#doCreateControls(org.eclipse.swt.widgets.Composite, org.eclipse.core.databinding.DataBindingContext)
	 */
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		
		// Sub Title Box
		GridLayoutFactory.fillDefaults()
		.margins(10, 10).applyTo(parent);

		Group editApplicationEnvironmentalVariableGroup = new Group(parent, SWT.NONE);
		editApplicationEnvironmentalVariableGroup.setText(
				ApplicationEnvironmentalVariableEditWizardPageModel.PROPERTY_GRID_TITLE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(editApplicationEnvironmentalVariableGroup);
		GridLayoutFactory.fillDefaults()
			.numColumns(4).margins(6, 6).applyTo(editApplicationEnvironmentalVariableGroup);
		
		// Variable Name Input
		Label nameLabel = new Label(editApplicationEnvironmentalVariableGroup, SWT.NONE);
		nameLabel.setText(
				ApplicationEnvironmentalVariableEditWizardPageModel.PROPERTY_NAME_INPUT_TITLE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);
		
		Text nameText = new Text(editApplicationEnvironmentalVariableGroup, SWT.BORDER);
		nameText.setEditable(false);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
			.to(BeanProperties.value(
					ApplicationEnvironmentalVariableEditWizardPageModel.PROPERTY_VARIABLE_NAME).observe(pageModel))
			.in(dbc);
		ControlDecorationSupport.create(
			nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		//Variable Value Input
		Label valueLabel = new Label(editApplicationEnvironmentalVariableGroup, SWT.NONE);
		valueLabel.setText(
				ApplicationEnvironmentalVariableEditWizardPageModel.PROPERTY_VALUE_INPUT_TITLE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);
		
		Text valueText = new Text(editApplicationEnvironmentalVariableGroup, SWT.BORDER);
		valueText.setFocus();
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(valueText);
		Binding valeuBinding = ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(valueText))
			.to(BeanProperties.value(
					ApplicationEnvironmentalVariableEditWizardPageModel.PROPERTY_VARIABLE_VALUE).observe(pageModel))
			.in(dbc);
		ControlDecorationSupport.create(
			valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
	}

	/**
	 * @return the pageModel
	 */
	public ApplicationEnvironmentalVariableEditWizardPageModel getPageModel() {
		return pageModel;
	}
	
}
