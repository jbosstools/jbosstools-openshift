/*******************************************************************************
 * Copyright (c) 2015-2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validate label values confirm to the format accepted
 * by OpenShift
 * 
 * @author Jeff Cantrill
 */
public class EnvironmentVarKeyValidator implements IValidator<String> {

	private static final Pattern CIDENTIFIER_REGEXP = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

	private static final IStatus NAME_IS_USED_ERROR = 
			ValidationStatus.error("An environment variable with this name already exists");
	private final IStatus failedStatus;
	private final Collection<String> usedKeys;

	public EnvironmentVarKeyValidator(Collection<String> usedKeys) {
		this("environment variable name", usedKeys);
	}

	public EnvironmentVarKeyValidator(String element, Collection<String> usedKeys) {
		this.failedStatus = ValidationStatus.error(NLS.bind(
				"A valid {0} is alphanumeric (a-z, and 0-9), "
				+ "including the character '_', allowed anywhere except first position.", 
				element));
		this.usedKeys = usedKeys != null ? usedKeys : new ArrayList<>(0);
	}

	@Override
	public IStatus validate(String value) {
		if (StringUtils.isBlank(value)) {
			return ValidationStatus.cancel("Please provide a key name.");
		}
		if (!CIDENTIFIER_REGEXP.matcher(value).matches()) {
			return failedStatus;
		}
		if (usedKeys.contains(value)) {
			return NAME_IS_USED_ERROR;
		}

		return ValidationStatus.OK_STATUS;
	}
}
