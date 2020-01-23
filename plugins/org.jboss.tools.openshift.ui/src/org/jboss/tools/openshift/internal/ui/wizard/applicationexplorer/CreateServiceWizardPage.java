/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Red Hat Developers
 *
 */
public class CreateServiceWizardPage extends AbstractOpenShiftWizardPage {

	private CreateServiceModel model;

	protected CreateServiceWizardPage(IWizard wizard, CreateServiceModel model) {
		super("Create service", "Specify a name for your service and choose a template to start from.", "Create service", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label serviceNameLabel = new Label(parent, SWT.NONE);
		serviceNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceNameLabel);
		Text serviceNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(serviceNameText);

		ISWTObservableValue<String> serviceNameObservable = WidgetProperties.text(SWT.Modify).observe(serviceNameText);
		Binding serviceNameBinding = ValueBindingBuilder.bind(serviceNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_SERVICE_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(serviceNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Label serviceTemplatesLabel = new Label(parent, SWT.NONE);
		serviceTemplatesLabel.setText("Component type:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceTemplatesLabel);
		Combo serviceTemplatesCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(serviceTemplatesCombo);
		ComboViewer serviceTemplatesComboViewer = new ComboViewer(serviceTemplatesCombo);
		serviceTemplatesComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		serviceTemplatesComboViewer.setLabelProvider(new ServiceTemplateColumLabelProvider());
		serviceTemplatesComboViewer.setInput(model.getServiceTemplates());
		Binding serviceTemplatesBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(serviceTemplatesComboViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a template.")))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_SELECTED_SERVICE_TEMPLATE, ServiceTemplate.class)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(serviceTemplatesBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		
		Label applicationLabel = new Label(parent, SWT.NONE);
		applicationLabel.setText("Application:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(applicationLabel);
		Text applicationNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(applicationNameText);

		ISWTObservableValue<String> applicationNameObservable = WidgetProperties.text(SWT.Modify).observe(applicationNameText);
		Binding applicationNameBinding = ValueBindingBuilder.bind(applicationNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify an application"))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_APPLICATION_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(applicationNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		if (StringUtils.isNotBlank(model.getApplicationName())) {
			applicationNameText.setEnabled(false);
		}
}
	
	/**
	 * @return
	 */
	public boolean finish() {
		try {
			model.getOdo().createService(model.getProjectName(), model.getApplicationName(), model.getSelectedServiceTemplate().getName(), model.getSelectedServiceTemplate().getPlan(), model.getServiceName());
			return true;
		} catch (IOException e) {
			setErrorMessage(e.getLocalizedMessage());
			return false;
		}
	}
}
