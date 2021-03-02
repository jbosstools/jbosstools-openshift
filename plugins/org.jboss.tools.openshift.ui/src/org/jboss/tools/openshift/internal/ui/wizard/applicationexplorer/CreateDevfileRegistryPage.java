/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.validator.URLValidator;

/**
 * @author Red Hat Developers
 *
 */
public class CreateDevfileRegistryPage extends AbstractOpenShiftWizardPage {
  
	private CreateDevfileRegistryModel model;

	protected CreateDevfileRegistryPage(IWizard wizard, CreateDevfileRegistryModel model) {
		super("Create devfile registry", "Specify a name for the registry, and enter the URL to the registry.", "Create devfile registry", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(nameLabel);
		Text nameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(nameText);

		ISWTObservableValue<String> nameObservable = WidgetProperties.text(SWT.Modify).observe(nameText);
		Binding nameBinding = ValueBindingBuilder.bind(nameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(CreateDevfileRegistryModel.PROPERTY_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

    Label urlLabel = new Label(parent, SWT.NONE);
    urlLabel.setText("URL:");
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(urlLabel);
    Text urlText = new Text(parent, SWT.BORDER);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
        .applyTo(urlText);

    ISWTObservableValue<String> urlObservable = WidgetProperties.text(SWT.Modify).observe(urlText);
    Binding urlBinding = ValueBindingBuilder.bind(urlObservable)
        .validatingAfterGet(new URLValidator("registry", false))
        .to(BeanProperties.value(CreateDevfileRegistryModel.PROPERTY_URL).observe(model))
        .in(dbc);
    ControlDecorationSupport.create(urlBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		
		Label secureLabel = new Label(parent, SWT.NONE);
		secureLabel.setText("Secure:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(secureLabel);
		Button secureButton = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(secureButton);

		ISWTObservableValue<Boolean> secureObservable = WidgetProperties.buttonSelection().observe(secureButton);
		Binding secureBinding = ValueBindingBuilder.bind(secureObservable)
				.to(BeanProperties.value(CreateDevfileRegistryModel.PROPERTY_SECURE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(secureBinding, SWT.LEFT | SWT.TOP);
		}
}
