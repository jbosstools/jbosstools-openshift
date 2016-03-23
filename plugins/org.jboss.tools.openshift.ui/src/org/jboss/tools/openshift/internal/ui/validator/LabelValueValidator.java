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

	/**
	 * This method must be in agreement with LABEL_REGEXP
	 * @param c
	 * @return
	 */
	protected static boolean isAlphaNumeric(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
	}

	/**
	 * This method must be in agreement with LABEL_REGEXP
	 * @param c
	 * @return
	 */
	protected static boolean isAlphaNumericOrValueSeparator(char c) {
		return isAlphaNumeric(c) || c == '.' || c == '-' || c == '_';
	}

	public static final String valueDescription = "A valid {0} must be 63 characters or less\n" +
			"and must begin and end with an alphanumeric character ([a-z0-9A-Z]),\n" +
			"with dashes (-), underscores (_), dots (.), and alphanumerics between.";

	public static final String defaultType = "label value";

	private final IStatus FAILED;
	protected String type;
	
	public LabelValueValidator() {
		this(defaultType);
	}
	
	public LabelValueValidator(String element) {
		type = element;
		FAILED = ValidationStatus.error(NLS.bind(valueDescription, type));
	}

	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String)) {
			return ValidationStatus.cancel(getValueIsNotAStringMessage());
		}
		String value = (String) paramObject;
		if(StringUtils.isBlank(value))
			return ValidationStatus.cancel(NLS.bind("{0} is required.", type));
		if(value.length() > LABEL_MAXLENGTH) {
			return getSizeConstraintError();
		}
		if(!LABEL_REGEXP.matcher(value).matches()) {
			return getLabelRegexError(value, type);
		}
		
		return ValidationStatus.OK_STATUS;
	}

	protected String getValueIsNotAStringMessage() {
		return NLS.bind("{0} is not an instance of a string", type);
	}

	/**
	 * Detailed status for the case LABEL_REGEXP.matcher(value).matches() = false
	 * This method assumes that regexp match is failed!
	 */
	protected IStatus getLabelRegexError(String value, String type) {
		//1. Check the first character
		if(!isAlphaNumeric(value.charAt(0))) {
			return ValidationStatus.error(NLS.bind("A valid {0} must begin with an alphanumeric character", type)); 
		}
		if(value.length() > 2) {
			//2. Check middle characters
			for (int i = 1; i < value.length() - 1; i++) {
				if(!isAlphaNumericOrValueSeparator(value.charAt(i))) {
					return ValidationStatus.error(NLS.bind("A character ''{0}'' is not allowed in {1}", value.substring(i, i + 1), type));
				}
			}
		}
		if(value.length() > 1 && !isAlphaNumeric(value.charAt(value.length() - 1))) {
			//3. Check the last character
			return ValidationStatus.error(NLS.bind("A valid {0} must end with an alphanumeric character", type));
		}
		//4. Should not happen.
		return ValidationStatus.error(NLS.bind("{0} is not valid.", type));
	}
	
	protected boolean validateLabel(String value) {
		if (value.length() > LABEL_MAXLENGTH) {
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
