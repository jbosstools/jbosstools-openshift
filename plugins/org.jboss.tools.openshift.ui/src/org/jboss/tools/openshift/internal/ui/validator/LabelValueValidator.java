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
 * Validate label values confirm to the format accepted
 * by OpenShift
 * 
 * @author Jeff Cantrill
 */
public class LabelValueValidator implements IValidator {
	
	public static final int LABEL_MAXLENGTH = 63;
	private static final Pattern LABEL_REGEXP = Pattern.compile("^(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$");

	private final String failureMessage = "A valid {0} must be 63 characters or less and must begin and end with an alphanumeric character " + 
			"([a-z0-9A-Z]) with dashes (-), underscores (_), dots (.), and alphanumerics between.";

	private final IStatus FAILED;
	protected String type;
	
	public LabelValueValidator() {
		this("label value");
	}
	
	public LabelValueValidator(String element) {
		type = element;
		FAILED = ValidationStatus.error(NLS.bind(failureMessage, type));
	}

	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String))
			return ValidationStatus.cancel(NLS.bind("{0} is not an instance of a string", type));
		String value= (String) paramObject;
		if(StringUtils.isBlank(value))
			return getPatternConstraintError();
		if(value.length()  > LABEL_MAXLENGTH) {
			return getSizeConstraintError();
		}
		if(!LABEL_REGEXP.matcher(value).matches()) {
			return getPatternConstraintError();
		}
		
		return ValidationStatus.OK_STATUS;
	}
	
	protected boolean validateLabel(String value) {
        if (value.length()  > LABEL_MAXLENGTH) { 
      	  return false; 
  	  }
        return LABEL_REGEXP.matcher(value).matches();
	}
	
	protected IStatus getSizeConstraintError() {
		return ValidationStatus.error(NLS.bind("Maximum length allowed is {0} characters for {1}", LABEL_MAXLENGTH, type));
	}

	protected IStatus getPatternConstraintError() {
		return getFailedStatus();
	}
	
	protected IStatus getFailedStatus() {
		return FAILED;
	}
}
