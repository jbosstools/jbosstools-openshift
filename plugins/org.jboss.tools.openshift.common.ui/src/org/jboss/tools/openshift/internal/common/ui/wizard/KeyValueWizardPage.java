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

import org.apache.commons.lang.StringUtils;
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

	private static final int KEYVALUE_GROUP_WIDTH = 550;

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
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(parent);

		Group group = new Group(parent, SWT.NONE);
		group.setText(model.getGroupLabel());
		group.setLayout(new GridLayout());
		GridDataFactory.fillDefaults()
			.hint(KEYVALUE_GROUP_WIDTH, SWT.DEFAULT).align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(group);

		Composite composite = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(composite);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).margins(10, 10)
			.applyTo(composite);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(model.getKeyLabel() + ":"); //$NON-NLS-1$
		if (model.getKeyDescription() != null) {
			nameLabel.setToolTipText(model.getKeyDescription());
		}
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);

		Text keyText = new Text(composite, SWT.BORDER);
		keyText.setEditable(model.isKeyEditable());

		IObservableValue<String> keyTextObservable = WidgetProperties.text(SWT.Modify).observe(keyText);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(keyText);
		Binding keyBinding = ValueBindingBuilder
				.bind(keyTextObservable)
				.validatingAfterConvert(model.getKeyAfterConvertValidator())
				.to(BeanProperties.value(IKeyValueWizardModel.PROPERTY_KEY).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(keyBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		Label valueLabel = new Label(composite, SWT.NONE);
		valueLabel.setText(model.getValueLabel() + ":"); //$NON-NLS-1$
		if (model.getValueDescription() != null) {
			valueLabel.setToolTipText(model.getValueDescription());
		}
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(valueLabel);

		Text valueText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(valueText);
		IObservableValue<String> valueTextObservable = WidgetProperties.text(SWT.Modify).observe(valueText);
		Binding valeuBinding = ValueBindingBuilder
				.bind(valueTextObservable)
				.validatingAfterConvert(model.getValueAfterConvertValidator())
				.to(BeanProperties.value(IKeyValueWizardModel.PROPERTY_VALUE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(valeuBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		if (!StringUtils.isEmpty(initialKey)) {
			DataChangedValidator validator = new DataChangedValidator(keyTextObservable, valueTextObservable);
			dbc.addValidationStatusProvider(validator);
		}

		initFocus(keyText, valueText);
	}

	private void initFocus(Text keyText, Text valueText) {
		if (!model.isKeyEditable() 
				|| !StringUtils.isEmpty(keyText.getText())) {
			valueText.forceFocus(); //if name is set, it is more important to modify value.
			if (!StringUtils.isEmpty(valueText.getText())) {
				valueText.setSelection(0, valueText.getText().length());
			}
		}
	}

	class DataChangedValidator extends MultiValidator {
		IObservableValue<String> keyTextObservable;
		IObservableValue<String> valueTextObservable;

		public DataChangedValidator(IObservableValue<String> keyTextObservable, IObservableValue<String> valueTextObservable) {
			this.keyTextObservable = keyTextObservable;
			this.valueTextObservable = valueTextObservable;
		}

		@Override
		protected IStatus validate() {
			String key = keyTextObservable.getValue();
			String value = valueTextObservable.getValue();
			if (initialKey != null 
					&& initialKey.equals(key) 
					&& initialValue != null
					&& initialValue.equals(value)) {
				return ValidationStatus.cancel("Provide new values.");
			}
			return ValidationStatus.ok();
		}
	}
}
