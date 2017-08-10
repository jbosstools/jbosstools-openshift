/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * A validator whose state may be observed.
 * 
 * @author Andre Dietisheim
 */
public class DisablableRequiredStringMultiValidator extends RequiredStringMultiValidator {

	private IObservableValue<Boolean> disabledObservable;

	public DisablableRequiredStringMultiValidator(IObservableValue<String> value, IObservableValue<Boolean> disabledObservable, String errorMessage) {
		super(errorMessage, value);
		this.disabledObservable = disabledObservable;
	}

	@Override
	protected IStatus validate() {
		if (!isDisabled()) {
			return super.validate();
		}
		return ValidationStatus.ok();
	}

	protected boolean isDisabled() {
		Boolean disabled = this.disabledObservable.getValue();
		return Boolean.TRUE.equals(disabled);
	}	
}		
