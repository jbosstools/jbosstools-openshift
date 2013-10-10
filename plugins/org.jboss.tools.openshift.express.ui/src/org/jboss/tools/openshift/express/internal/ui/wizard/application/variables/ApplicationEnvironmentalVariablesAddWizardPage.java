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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * 
 */
public class ApplicationEnvironmentalVariablesAddWizardPage extends AbstractOpenShiftWizardPage {

	/**
	 * Constructs a new instance of
	 * ApplicationEnvironmentalVariablesAddWizardPage
	 * 
	 * @param title
	 * @param description
	 * @param pageName
	 * @param wizard
	 */
	public ApplicationEnvironmentalVariablesAddWizardPage(String title, String description, String pageName,
			IWizard wizard) {
		super(title, description, pageName, wizard);
		pageModel = new ApplicationEnvironmentalVariablesAddWizardPageModel();
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
		/*
		 * Binding nameBinding = ValueBindingBuilder
		 * .bind(WidgetProperties.text(SWT.Modify).observe(nameText))
		 * .to(BeanProperties
		 * .value(pageModel.getPROPERTY_VARIABLE_NAME()).observe(pageModel))
		 * .notUpdatingParticipant() .in(dbc); ControlDecorationSupport.create(
		 * nameBinding, SWT.LEFT | SWT.TOP, null, new
		 * RequiredControlDecorationUpdater());
		 */

		Label valueLabel = new Label(addApplicationEnvironmentalVariableGroup, SWT.NONE);
		valueLabel.setText("New Variable Value:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);

		Text valueText = new Text(addApplicationEnvironmentalVariableGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(valueText);
		/*
		 * Binding valeuBinding = ValueBindingBuilder
		 * .bind(WidgetProperties.text(SWT.Modify).observe(valueText))
		 * .to(BeanProperties
		 * .value(pageModel.getPROPERTY_VARIABLE_VALUE()).observe(pageModel))
		 * .notUpdatingParticipant() .in(dbc);
		 * 
		 * ControlDecorationSupport.create( valeuBinding, SWT.LEFT | SWT.TOP,
		 * null, new RequiredControlDecorationUpdater());
		 */
	}

	private ApplicationEnvironmentalVariablesAddWizardPageModel pageModel;

}
