/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui;

import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;

/**
 * @author André Dietisheim
 */
public class ValidationStatusControlDecoration {

	private IObservableValue validationStatus;

	public ValidationStatusControlDecoration(ValidationStatusProvider provider) {
		this.validationStatus = provider.getValidationStatus();
	}

	public void showFor(Control control, int position) {
		ControlDecoration decoration = createDecoration(control, position);
		IValueChangeListener validationStatusListener = onValidationStatusChanged(decoration);
		
		validationStatus.addValueChangeListener(validationStatusListener);
		control.addDisposeListener(onControlDisposed(validationStatusListener));

	}
	
	private ControlDecoration createDecoration(Control control, int position) {
		ControlDecoration controlDecoration = new ControlDecoration(control, position);
		FieldDecoration fieldDecoration =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
		controlDecoration.setImage(fieldDecoration.getImage());
		if (validationStatus.getValue() instanceof IStatus) {
			showDecoration(controlDecoration, (IStatus) validationStatus.getValue());
		}
		return controlDecoration;
	}

	private DisposeListener onControlDisposed(final IValueChangeListener validationStatusListener) {
		return new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				validationStatus.removeValueChangeListener(validationStatusListener);
			}
		};
	}

	private IValueChangeListener onValidationStatusChanged(final ControlDecoration controlDecoration) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				if (!(event.diff.getNewValue() instanceof IStatus)) {
					return;
				}
				IStatus validationStatus = (IStatus) event.diff.getNewValue();
				showDecoration(controlDecoration, validationStatus);
			}
		};
	}

	private void showDecoration(final ControlDecoration controlDecoration, IStatus validationStatus) {
		if (validationStatus.isOK()) {
			controlDecoration.hide();
		} else {
			controlDecoration.show();
		}
	}

}
