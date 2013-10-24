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
package org.jboss.tools.openshift.express.internal.ui.wizard.environment;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredStringValidator;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Martes G Wigglesworth <martes.wigglesworth@redhat.com>
 * @author Martin Rieman
 * @author Andre Dietisheim
 * 
 */
public class EnvironmentVariableWizardPage extends AbstractOpenShiftWizardPage {

	private EnvironmentVariableWizardModel model;

	public EnvironmentVariableWizardPage(EnvironmentVariableWizardModel model, IWizard wizard) {
		super((model.isEditing()? "Edit an existing environment variable" : "Add a new environment variable"),
				"Please choose a name and a value for environment variable", "",
				wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group addApplicationEnvironmentVariableGroup = new Group(parent, SWT.NONE);
		addApplicationEnvironmentVariableGroup.setText("Enviroment Variable");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(addApplicationEnvironmentVariableGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(4).margins(6, 6).applyTo(addApplicationEnvironmentVariableGroup);

		Label nameLabel = new Label(addApplicationEnvironmentVariableGroup, SWT.NONE);
		nameLabel.setText("Name:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(addApplicationEnvironmentVariableGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
				.validatingAfterConvert(new RequiredStringValidator("Name") {

					@Override
					public IStatus validateString(String value) {
						if (model.isExistingName(value)) {
							return ValidationStatus.error(NLS.bind("There's already an environment variable with the name {0}", value));
						}
						if (!StringUtils.startsWithLetterOrUnderscore(value)
								|| !StringUtils.isAlphaNumericOrUnderscore(value)) {
							return ValidationStatus
									.error("Name can only contain letters, digits and underscore and can't begin with a digit.");
						}
						return ValidationStatus.ok();
					}

				})
				.to(BeanProperties.value(EnvironmentVariableWizardModel.PROPERTY_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Label valueLabel = new Label(addApplicationEnvironmentVariableGroup, SWT.NONE);
		valueLabel.setText("Value:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);

		Text valueText = new Text(addApplicationEnvironmentVariableGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(valueText);
		Binding valeuBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(valueText))
				.validatingAfterConvert(new RequiredStringValidator("Value"))
				.to(BeanProperties.value(EnvironmentVariableWizardModel.PROPERTY_VALUE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}
}
