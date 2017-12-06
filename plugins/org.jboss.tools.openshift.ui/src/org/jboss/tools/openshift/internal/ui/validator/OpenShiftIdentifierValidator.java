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
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class OpenShiftIdentifierValidator implements IValidator {

	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	@Override
	public IStatus validate(Object value) {
		if (!(value instanceof String) || StringUtils.isEmpty((String) value)) {
			return ValidationStatus.error("Please provide an alphanumeric identifier.");
		}
		if (!IDENTIFIER_PATTERN.matcher((String) value).matches()) {
			return ValidationStatus.error("Please provide an identier that starts with alphabetic character or '_', "
					+ "followed by a string of alphanumeric characters or '_'");
		}
		return ValidationStatus.ok();
	}

}
