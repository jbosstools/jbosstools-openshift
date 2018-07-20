/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * A multi validator that validates a given value observable if validation is
 * enabled by the given enablement observable. Validation happens based on the
 * given validation function.
 * 
 * @author Andre Dietisheim
 */
public class DisableableMultiValitdator<T> extends MultiValidator {

	private final IObservableValue<Boolean> enablementObservable;
	private final IObservableValue<T> valueObservable;
	private IValidator validator;
	private final IObservableList<IObservable> targets;

	public DisableableMultiValitdator(IObservableValue<Boolean> enablementObservable,
			IObservableValue<T> valueObservable, IValidator validator) {
		this.enablementObservable = enablementObservable;
		this.valueObservable = valueObservable;
		this.targets = new WritableList<>();
		targets.add(valueObservable);
		this.validator = validator;
	}

	@Override
	protected IStatus validate() {
		Boolean enabled = enablementObservable.getValue();
		T value = valueObservable.getValue();

		if (Boolean.FALSE.equals(enabled)) {
			IStatus valueValidity = validator.validate(value);
			if (!valueValidity.isOK()) {
				return valueValidity;
			}
		}

		return ValidationStatus.ok();
	}

	@Override
	public IObservableList<IObservable> getTargets() {
		return targets;
	}
}
