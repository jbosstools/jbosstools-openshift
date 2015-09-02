/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.wizard;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;

/**
 * A generic page for editing key value pairs (e.g. env vars; labels) 
 * 
 * @author jeff.cantrill
 */
public class KeyValueWizardPage<T extends IKeyValueItem> extends AbstractOpenShiftWizardPage {

	private IKeyValueWizardModel<T> model;

	public KeyValueWizardPage(IWizard wizard, IKeyValueWizardModel<T> model) {
		super(model.getTitle(), model.getDescription(), "", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group group = new Group(parent, SWT.NONE);
		group.setText(model.getGroupLabel());
		group.setLayout(new GridLayout());
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(group);

		Composite composite = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(composite);
		GridLayoutFactory.fillDefaults()
			.numColumns(4).margins(25, 25).applyTo(composite);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(model.getKeyLabel()+":");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text nameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(nameText))
				.validatingAfterConvert(model.getKeyAfterConvertValidator())
				.to(BeanProperties.value(IKeyValueWizardModel.PROPERTY_KEY).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				nameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Label valueLabel = new Label(composite, SWT.NONE);
		valueLabel.setText(model.getValueLabel()+":");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);

		Text valueText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(valueText);
		Binding valeuBinding = ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(valueText))
				.validatingAfterConvert(model.getValueAfterConvertValidator())
				.to(BeanProperties.value(IKeyValueWizardModel.PROPERTY_VALUE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(
				valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
	}
}
