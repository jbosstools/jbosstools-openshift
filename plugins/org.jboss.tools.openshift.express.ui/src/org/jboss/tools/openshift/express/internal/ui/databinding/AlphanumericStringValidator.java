/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.databinding;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class AlphanumericStringValidator extends RequiredStringValidator {

	public AlphanumericStringValidator(String fieldName) {
		super(fieldName);
	}

	@Override
	public IStatus validateString(String value) {
		if (!StringUtils.isAlphaNumeric(value)) {
			return ValidationStatus.error("You have to provide an alphanumeric " + getFieldName());
		}
		return ValidationStatus.ok();
	}

}
