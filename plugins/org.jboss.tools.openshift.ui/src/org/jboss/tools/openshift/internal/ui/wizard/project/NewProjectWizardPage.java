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
package org.jboss.tools.openshift.internal.ui.wizard.project;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.validator.ProjectDisplayNameValidator;
import org.jboss.tools.openshift.internal.ui.validator.ProjectNameValidator;

import com.openshift.restclient.model.IProject;

/**
 * @author jeff.cantrill
 */
public class NewProjectWizardPage extends AbstractOpenShiftWizardPage {
	
	private NewProjectWizardModel model;

	public NewProjectWizardPage(NewProjectWizardModel model, IWizard wizard) {
		this("New OpenShift Project", "Please provide name, display name and description", model, wizard);
	}

	protected NewProjectWizardPage(String title, String description, NewProjectWizardModel model, IWizard wizard) {
		super(title, description, "", wizard);
		this.model = model;
	}
	
	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(parent);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(2).applyTo(parent);

		// project name
		Label lblName = new Label(parent, SWT.NONE);
		lblName.setText(OpenShiftUIMessages.Name);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(lblName);
		lblName.setToolTipText(ProjectNameValidator.projectNameDescription);
		
		Text txtName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtName);
		IObservableValue nameTextObservable = WidgetProperties.text(SWT.Modify).observe(txtName);
		Binding nameBinding = ValueBindingBuilder
				.bind(nameTextObservable)
				.validatingAfterConvert(new ProjectNameValidator(getDescription(), model.getUnavailableNames()))
				.to(BeanProperties.value(NewProjectWizardModel.PROPERTY_PROJECT_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(nameBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());

		// display name
		Label lblDisplayName = new Label(parent, SWT.NONE);
		lblDisplayName.setText(OpenShiftUIMessages.DisplayName);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(lblDisplayName);
		
		Text txtDispalayName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtDispalayName);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(txtDispalayName))
			.validatingAfterConvert(new ProjectDisplayNameValidator())
			.to(BeanProperties.value(NewProjectWizardModel.PROPERTY_DISPLAY_NAME).observe(model))
			.in(dbc);

		// description
		Label lblDescription = new Label(parent, SWT.NONE);
		lblDescription.setText(OpenShiftUIMessages.Description);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(lblDisplayName);

		Text txtDescription = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(txtDescription);
		ValueBindingBuilder
			.bind(WidgetProperties.text(SWT.Modify).observe(txtDescription))
			.to(BeanProperties.value(NewProjectWizardModel.PROPERTY_DESCRIPTION).observe(model))
			.in(dbc);
	}

	public IProject getProject() {
		return model.getProject();
	}
	
	protected NewProjectWizardModel getModel() {
		return model;
	}
	
	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(IStatus.ERROR | IStatus.CANCEL, this, dbc);
	}

}
