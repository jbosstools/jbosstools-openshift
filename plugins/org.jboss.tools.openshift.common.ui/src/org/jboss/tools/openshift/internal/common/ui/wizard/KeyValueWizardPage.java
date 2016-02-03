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
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
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
	private String initialKey;
	private String initialValue;

	public KeyValueWizardPage(IWizard wizard, IKeyValueWizardModel<T> model) {
		super(model.getTitle(), model.getDescription(), "", wizard);
		this.model = model;
		initialKey = model.getKey();
		initialValue = model.getValue();
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(parent);

		Group group = new Group(parent, SWT.NONE);
		group.setText(model.getGroupLabel());
		group.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().hint(550, SWT.DEFAULT)
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
		nameText.setEditable(model.isKeyEditable());

		IObservableValue keyModel = BeanProperties.value(IKeyValueWizardModel.PROPERTY_KEY).observe(model);
		IObservableValue keyTextObservable = WidgetProperties.text(SWT.Modify).observe(nameText);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).span(3, 1).applyTo(nameText);
		Binding nameBinding = ValueBindingBuilder
				.bind(keyTextObservable)
				.validatingAfterConvert(model.getKeyAfterConvertValidator())
				.to(keyModel)
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
		IObservableValue valueModel = BeanProperties.value(IKeyValueWizardModel.PROPERTY_VALUE).observe(model);
		IObservableValue valueTextObservable = WidgetProperties.text(SWT.Modify).observe(valueText);
		Binding valeuBinding = ValueBindingBuilder
				.bind(valueTextObservable)
				.validatingAfterConvert(model.getValueAfterConvertValidator())
				.to(valueModel)
				.in(dbc);
		ControlDecorationSupport.create(
				valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		if(initialKey != null && !initialKey.isEmpty()) {
			DataChangedValidator validator = new DataChangedValidator(keyTextObservable, valueTextObservable);
			dbc.addValidationStatusProvider(validator);
		}
	}

	class DataChangedValidator extends MultiValidator {
		IObservableValue keyTextObservable;
		IObservableValue valueTextObservable;

		public DataChangedValidator(IObservableValue keyTextObservable, IObservableValue valueTextObservable) {
			this.keyTextObservable = keyTextObservable;
			this.valueTextObservable = valueTextObservable;
		}

		@Override
		protected IStatus validate() {
			String key = (String)keyTextObservable.getValue();
			String value = (String)valueTextObservable.getValue();
			if(initialKey != null && initialKey.equals(key)
					&& initialValue != null && initialValue.equals(value)) {
				return ValidationStatus.cancel("Provide new values.");
			}
			return ValidationStatus.ok();
		}
	}
}
