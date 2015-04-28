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

/**
 * Validate label values confirm to the format accepted
 * by OpenShift
 */
public class LabelValueValidator implements IValidator {

	public static final int LABEL_MAXLENGTH = 63;
	private static final Pattern LABEL_REGEXP = Pattern.compile("^(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$");

	private final String failureMessage = "A valid label value is an alphanumeric (a-z, and 0-9) string, with a maximum length of 63 characters, with the '-' character allowed anywhere except the first or last character.";

	protected IStatus FAILED;
	
	public LabelValueValidator() {
		FAILED = ValidationStatus.error(failureMessage);
	}
	
	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String))
			return FAILED;
		String value= (String) paramObject;
		if(StringUtils.isEmpty(value))
			return FAILED;
		
		return validateLabel(value) ? ValidationStatus.OK_STATUS : FAILED;
	}
	
	protected boolean validateLabel(String value) {
        if (value.length()  > LABEL_MAXLENGTH) { 
      	  return false; 
  	  }
        return LABEL_REGEXP.matcher(value).matches();
	}
	
	public IStatus getFailedStatus() {
		return FAILED;
	}
}
