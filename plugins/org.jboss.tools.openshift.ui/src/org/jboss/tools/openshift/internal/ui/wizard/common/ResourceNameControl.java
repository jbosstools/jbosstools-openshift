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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.ui.validator.ResourceNameValidator;

/**
 * Control for retrieving and validating a resource name
 * @author jeff.cantrill
 *
 */
public class ResourceNameControl {
	
	public static final String PROPERTY_RESOURCE_NAME = "resourceName";
	private static final String DEFAULT_LABEL = "Resource Name: ";
	private final String label;
	
	public ResourceNameControl() {
		this(DEFAULT_LABEL);
	}
	
	public ResourceNameControl(String label) {
		this.label = label;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void doCreateControl(Composite parent, DataBindingContext dbc, Object model) {
		
		//Resource Name
		final Label resourceNameLabel = new Label(parent, SWT.NONE);
		resourceNameLabel.setText(label);
		resourceNameLabel.setToolTipText("The name used to identify the resources that will support the deployed image.");
		layoutLabel(resourceNameLabel);

		
		final Text resourceNameText = new Text(parent, SWT.BORDER);
		layoutText(resourceNameText);
		final IObservableValue resourceNameTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(resourceNameText);
		final Binding nameBinding = ValueBindingBuilder
				.bind(resourceNameTextObservable)
				.to(BeanProperties.value(PROPERTY_RESOURCE_NAME).observe(model))
				.in(dbc);
		dbc.addValidationStatusProvider(new ResourceNameValidator(resourceNameTextObservable));
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
	}
	
	protected void layoutText(final Text resourceNameText) {
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(true, false)
			.span(2, 1)
			.applyTo(resourceNameText);
	}
	
	protected void layoutLabel(final Label resourceNameLabel) {
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER)
			.grab(false, false)
			.applyTo(resourceNameLabel);
	}
}
