/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

@SuppressWarnings("rawtypes")
public class EditValueDialog extends InputDialog {
	static final String PROVIDE_REQUIRED_VALUE = "{0} is a required value, please provide a value.";
	static final String PROVIDE_NEW_VALUE = "Please provide new value";

	DataBindingContext dbc = new DataBindingContext();
	InputModel model;

	String name;
	String initialValue;
	boolean required;

	public EditValueDialog(Shell shell, 
			String title, String message, String name, String initialValue, boolean required) {
		super(shell, title, message, initialValue, new InputValidator());
		this.name = name;
		this.initialValue = initialValue;
		this.required = required;
		model = new InputModel();
	}

	@Override
	public InputValidator getValidator() {
		return (InputValidator)super.getValidator();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite control = (Composite)super.createDialogArea(parent);
		Text text = getFirstTextControl(control.getChildren());
		if(text != null) {
			IObservableValue modelObservable = BeanProperties.value(InputModel.VALUE).observe(model);
			IObservableValue textObservable = WidgetProperties.text(SWT.Modify).observe(text);
			ValueBindingBuilder.bind(textObservable).to(modelObservable).in(dbc);
			ValidationStatusProvider validator = new ValidationStatusProvider(textObservable);
			getValidator().setProvider(validator);
			dbc.addValidationStatusProvider(validator);
			ControlDecorationSupport.create(validator, SWT.LEFT | SWT.TOP);
			setErrorMessage(getValidator().isValid(initialValue));
		} else {
			//May happen only if InputDialog implementation changes.
			OpenShiftUIActivator.getDefault().getLogger().logError(new NullPointerException("Cannot find text widget."));
		}
		return control;
	}

	private Text getFirstTextControl(Control[] controls) {
		for (Control c: controls) {
			if(c instanceof Text) {
				return (Text)c;
			}
		}
		return null;
	}

	public static class InputModel extends ObservableUIPojo {
		static String VALUE = "value";
		
		String value = "";

		public String getValue() {
			return value;
		}
	
		public void setValue(String value) {
			this.value = value;
		}
	}

	static class InputValidator implements IInputValidator {
		ValidationStatusProvider provider;

		@Override
		public String isValid(String newText) {
			if(provider != null) {
				IStatus status = provider.validate();
				if(!status.isOK()) {
					return status.getMessage();
				}
			}
			return null;
		}
		
		public void setProvider(ValidationStatusProvider provider) {
			this.provider = provider;
		}
		
	}
	
	class ValidationStatusProvider extends MultiValidator {
		IObservableValue textObservable;

		ValidationStatusProvider(IObservableValue textObservable) {
			this.textObservable = textObservable;
		}
		
		@Override
		protected IStatus validate() {
			String text = (String)textObservable.getValue();
			if(text == null || text.isEmpty()) {
				if(required) {
					return ValidationStatus.error(NLS.bind(PROVIDE_REQUIRED_VALUE, name));
				}
			}
			if(text != null && text.equals(initialValue)) {
				return ValidationStatus.cancel(PROVIDE_NEW_VALUE);
			}
			return ValidationStatus.ok();
		}
		
	}

}
