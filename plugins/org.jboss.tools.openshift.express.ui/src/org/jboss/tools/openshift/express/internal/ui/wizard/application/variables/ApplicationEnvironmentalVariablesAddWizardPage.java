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
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * @author Martin Rieman
 * 
 */
public class ApplicationEnvironmentalVariablesAddWizardPage extends AbstractOpenShiftWizardPage {

	private ApplicationEnvironmentalVariablesAddWizardPageModel pageModel;
	/**
	 * Constructs a new instance of
	 * ApplicationEnvironmentalVariablesAddWizardPage
	 * 
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariablesAddWizardPage(ApplicationEnvironmentalVariableConfigurationWizardPageModel confPageModel, IWizard wizard) {
		super(ApplicationEnvironmentalVariablesAddWizardPageModel.PAGE_TITLE, 
				/*
				 * Messy work-around to get the dynamicly allocated application name. (IApplication.getName())
				 */
				ApplicationEnvironmentalVariablesAddWizardPageModel.PAGE_DESCRIPTION+confPageModel.getIApplication().getName(), 
				ApplicationEnvironmentalVariablesAddWizardPageModel.PAGE_NAME, wizard);
		pageModel = new ApplicationEnvironmentalVariablesAddWizardPageModel(confPageModel);
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
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {

		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group addApplicationEnvironmentalVariableGroup = new Group(parent, SWT.NONE);
		addApplicationEnvironmentalVariableGroup.setText("New Variable Edit Dialog");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(addApplicationEnvironmentalVariableGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(4).margins(6, 6).applyTo(addApplicationEnvironmentalVariableGroup);

		Label nameLabel = new Label(addApplicationEnvironmentalVariableGroup, SWT.NONE);
		nameLabel.setText("New Variable Name:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(addApplicationEnvironmentalVariableGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
				.to(BeanProperties.value(ApplicationEnvironmentalVariablesAddWizardPageModel.PROPERTY_VARIABLE_NAME).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		
		Label valueLabel = new Label(addApplicationEnvironmentalVariableGroup, SWT.NONE);
		valueLabel.setText("New Variable Value:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);

		Text valueText = new Text(addApplicationEnvironmentalVariableGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(valueText);
		Binding valeuBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(valueText))
				.to(BeanProperties.value(ApplicationEnvironmentalVariablesAddWizardPageModel.PROPERTY_VARIABLE_VALUE).observe(pageModel))
				.in(dbc);
		ControlDecorationSupport.create(
				valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}

	/**
	 * @return the pageModel
	 */
	public ApplicationEnvironmentalVariablesAddWizardPageModel getPageModel() {
		return pageModel;
	}

}
