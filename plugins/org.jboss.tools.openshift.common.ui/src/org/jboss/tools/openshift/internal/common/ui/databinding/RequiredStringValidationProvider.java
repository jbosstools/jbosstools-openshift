/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
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

/**
 * A validator whose state may be observed.
 * 
 * @author Andre Dietisheim
 */
public class RequiredStringValidationProvider extends MultiValidator {

	private IObservableValue observableValue;
	private String name;

	public RequiredStringValidationProvider(IObservableValue value, String name) {
		this.observableValue = value;
		observableValue.getValue();
		this.name = name;
	}

	@Override
	protected IStatus validate() {
		Object value = observableValue.getValue();
		if (!(value instanceof String)
				|| ((String) value).isEmpty()) {
			return ValidationStatus.cancel("You have to provide a " + name);
		}
		return ValidationStatus.ok();
	}

}		
