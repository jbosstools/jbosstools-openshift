/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
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
public class EnvironmentVarKeyValidator implements IValidator {

	private static final Pattern CIDENTIFIER_REGEXP = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

	private final String failureMessage = "A valid {0} is alphanumeric (a-z, and 0-9), "
			+ "including the character '_', allowed anywhere except first position.";

	private final IStatus FAILED;
	private static final IStatus NAME_IS_USED_ERROR = ValidationStatus.error("An environment variable with this name already exists");
	private Collection<String> usedKeys;
	
	public EnvironmentVarKeyValidator(Collection<String> usedKeys) {
		this("environment variable name", usedKeys);
	}
	
	public EnvironmentVarKeyValidator(String element, Collection<String> usedKeys) {
		FAILED = ValidationStatus.error(NLS.bind(failureMessage, element));
		this.usedKeys = usedKeys != null ? usedKeys : new ArrayList<>(0);
	}

	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String))
			return ValidationStatus.cancel("Value is not an instance of a string");
		String value= (String) paramObject;
		if(!CIDENTIFIER_REGEXP.matcher(value).matches()) {
			return FAILED;
		}
		if(usedKeys.contains(value)) {
			return NAME_IS_USED_ERROR;
		}
		
		return ValidationStatus.OK_STATUS;
	}
}
