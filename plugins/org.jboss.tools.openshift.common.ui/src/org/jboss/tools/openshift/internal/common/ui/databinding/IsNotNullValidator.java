/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * @author Andre Dietisheim
 */
public class IsNotNullValidator implements IValidator {

	private IStatus invalidStatus;

	public IsNotNullValidator(IStatus invalidStatus) {
		this.invalidStatus = invalidStatus;
	}

	@Override
	public IStatus validate(Object value) {
		if (value == null) {
			return invalidStatus;
		}
		return ValidationStatus.ok();
	}
}
