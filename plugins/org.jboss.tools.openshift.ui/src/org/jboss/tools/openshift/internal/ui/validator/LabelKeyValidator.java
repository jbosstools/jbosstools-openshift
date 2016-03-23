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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validates label keys conform to the format that
 * is accepted by OpenShift
 * 
 * @author Jeff Cantrill
 */
public class LabelKeyValidator extends LabelValueValidator {
	
	public static final int SUBDOMAIN_MAXLENGTH = 253;

	private static final Pattern SUBDOMAIN_REGEXP = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");

	/**
	 * This method must be in agreement with SUBDOMAIN_REGEXP
	 * @param c
	 * @return
	 */
	private static boolean isLowerCaseAlphaNumeric(char c) {
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
	}

	/**
	 * This method must be in agreement with SUBDOMAIN_REGEXP
	 * @param c
	 * @return
	 */
	private static boolean isLowerCaseAlphaNumericOrDash(char c) {
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
	}

	/**
	 * This method must be in agreement with SUBDOMAIN_REGEXP
	 * @param domain
	 * @return
	 */
	private static String[] getSubdomainParts(String domain) {
		return domain.split("\\.");
	}


	private Collection<String> readonlykeys;
	private Collection<String> usedKeys;

	public static final String keyDescription = "A valid label key has the form [domain/]name where name is required,\n"
			+ "must be 63 characters or less, beginning and ending with an alphanumeric character ([a-z0-9A-Z])\n"
			+ "with dashes (-), underscores (_), dots (.), and alphanumerics between.\n"
			+ "A domain is an optional sequence of names separated "
			+ "by the '.' character with a maximum length of 253 characters.";
	
	private final IStatus FAILED = ValidationStatus.error(keyDescription);
	
	public LabelKeyValidator(Collection<String> readonlykeys, Collection<String> usedKeys) {
		super("label key");
		this.readonlykeys = readonlykeys != null ? readonlykeys : new ArrayList<String>(0);
		this.usedKeys = usedKeys != null ? usedKeys : new ArrayList<String>(0);
	}
	
	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String)) {
			return ValidationStatus.cancel(getValueIsNotAStringMessage());
		}
		String value = (String) paramObject;
		if(StringUtils.isEmpty(value)) {
			return ValidationStatus.cancel(NLS.bind("{0} is required", type));
		}
		if(readonlykeys.contains(value)) {
			return ValidationStatus.error("Adding a label with a key that is the same as a readonly label is not allowed");
		}
		if(usedKeys.contains(value)) {
			return ValidationStatus.error("A label with this key exists");
		}
		if(value.endsWith("/")) {
			//Special case, split will ignore the last '/'.
			return ValidationStatus.error(NLS.bind("A valid {0} must end with an alphanumeric character", type));
		}
		String [] parts = value.split("/");
		switch(parts.length) {
			case 1:
	            return super.validate(value);
			case 2:
				if(!validateSubdomain(parts[0])) {
					if(parts[0].length() > SUBDOMAIN_MAXLENGTH) {
						return ValidationStatus.error(NLS.bind("Maximum length of domain of label key allowed is {0} characters", SUBDOMAIN_MAXLENGTH)); 
					}
					return getSubdomainRegexError(parts[0], "domain of " + type);
				}
				if(!validateLabel(parts[1])) {
					if (value.length() > LABEL_MAXLENGTH) {
						return ValidationStatus.error(NLS.bind("Maximum length of name of label key allowed is {0} characters", LABEL_MAXLENGTH)); 
					}
					return getLabelRegexError(parts[1], "name of " + type);
				}
				return ValidationStatus.OK_STATUS;
            default:
		}
		return ValidationStatus.error(NLS.bind("More than one '/' is not allowed. A valid {0} has an optional domain separated by '/' from name", type));
	}
	
	private boolean validateSubdomain(String value) {
		if(value.length() > SUBDOMAIN_MAXLENGTH) {
			return false;
		}
		return SUBDOMAIN_REGEXP.matcher(value).matches();
	}

	/**
	 * Detailed status for the case SUBDOMAIN_MAXLENGTH.matcher(value).matches() = false
	 * This method assumes that regexp match is failed!
	 */
	protected IStatus getSubdomainRegexError(String value, String type) {
		if(value.isEmpty()) {
			return ValidationStatus.error(NLS.bind("A valid {0} cannot be empty if '/' is present.", type));
		}
		if(value.endsWith(".")) {
			return ValidationStatus.error(NLS.bind("A valid {0} must end with a lower-case alphanumeric character.", type));
		}
		String[] sparts = getSubdomainParts(value);
		for (String s: sparts) {
			//1. Check that parts of domain are not empty.
			if(s.isEmpty()) {
				return ValidationStatus.error(NLS.bind("Two dots in a row are not allowed in {0}.", type));
			}
			//2. Check the first character
			if(!isLowerCaseAlphaNumeric(s.charAt(0))) {
				return ValidationStatus.error(NLS.bind("A valid part of {0} must begin with a lower-case alphanumeric.", type));
			}
			for (int i = 1; i < s.length() - 1; i++) {
				//3. Check middle characters
				if(!isLowerCaseAlphaNumericOrDash(s.charAt(i))) {
					return ValidationStatus.error(NLS.bind("A character ''{0}'' is not allowed in {1}.", s.substring(i, i + 1), type));
				}
			}
			//4. Check the last character
			if(s.length() > 1 && !isLowerCaseAlphaNumeric(s.charAt(s.length() - 1))) {
				return ValidationStatus.error(NLS.bind("A valid part of {0} must end with a lower-case alphanumeric character.", type));
			}
		}
		//5. Should not happen.
		return ValidationStatus.error(NLS.bind("{0} is not valid.", type));
	}

	@Override
	protected IStatus getFailedStatus() {
		return FAILED;
	}
}
