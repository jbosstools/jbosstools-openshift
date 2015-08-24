/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validates the resource name for the generated resources.  Using a
 * service validation since that seems to be the limiting one.
 * 
 * @author Jeff Cantrill
 */
public class DeployImageNameValidator implements IValidator {

	private static final String CHAR_FMT = "[a-z0-9]";
	private static final String EXT_CHAR_FMT = "[-a-z0-9]";
	private static final int MAXLENGTH = 24;
	private static final Pattern REGEX = Pattern.compile("^[a-z](" + EXT_CHAR_FMT + "*" + CHAR_FMT +")?$");

	private final static String failureMessage = "A valid name is alphanumeric (a-z, and 0-9), "
			+ "with the character '-' allowed anywhere except first or last position.";

	private final IStatus FAILED;
	
	public DeployImageNameValidator() {
		FAILED = ValidationStatus.error(failureMessage);
	}
	
	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String))
			return ValidationStatus.cancel("Name is not an instance of a string");
		String value= (String) paramObject;
		if(StringUtils.isEmpty(value))
			return FAILED;
		if(value.length() > MAXLENGTH) {
			return ValidationStatus.error(NLS.bind("Maximum name length is {0} characters", MAXLENGTH));
		}

		if(!REGEX.matcher(value).matches()) {
			return FAILED;
		}
		return ValidationStatus.OK_STATUS;
	}

}
