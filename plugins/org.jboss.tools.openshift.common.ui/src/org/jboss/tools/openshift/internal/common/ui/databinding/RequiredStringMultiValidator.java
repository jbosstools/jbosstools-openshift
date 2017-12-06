/******************************************************************************* 
 * Copyright (c) 2015-2017 Red Hat, Inc. 
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
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * A validator whose state may be observed.
 * 
 * @author Andre Dietisheim
 */
public class RequiredStringMultiValidator extends MultiValidator {

	private IObservableValue<String> observableValue;
	private String name;
	private String errorMessage;

	public RequiredStringMultiValidator(IObservableValue<String> value, String name) {
		this(value, name, null);
	}

	public RequiredStringMultiValidator(String errorMessage, IObservableValue<String> value) {
		this(value, null, errorMessage);
	}

	protected RequiredStringMultiValidator(IObservableValue<String> value, String name, String errorMessage) {
		this.observableValue = value;
		this.name = name;
		this.errorMessage = errorMessage;
	}

	@Override
	protected IStatus validate() {
		String value = observableValue.getValue();
		if (!isValueProvided(value)) {
			return ValidationStatus.cancel(getErrorMessage());
		}
		return validateValue(value);
	}

	protected boolean isValueProvided(String value) {
		return !StringUtils.isEmpty(value);
	}

	protected IStatus validateValue(String value) {
		return ValidationStatus.ok();
	}

	protected String getErrorMessage() {
		if (errorMessage != null) {
			return errorMessage;
		} else {
			return "Please provide a value for " + name;
		}
	}

}
