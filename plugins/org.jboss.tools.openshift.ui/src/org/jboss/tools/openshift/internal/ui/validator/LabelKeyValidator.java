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

/**
 * Validates label keys conform to the format that
 * is accepted by OpenShift
 * 
 * @author Jeff Cantrill
 */
public class LabelKeyValidator extends LabelValueValidator {
	
	public static final int SUBDOMAIN_MAXLENGTH = 253;
	private static final Pattern SUBDOMAIN_REGEXP = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");
	private Collection<String> readonlykeys;

	private static final String failureMessage = "A valid object label has the form [domain/]name where a name is an alphanumeric (a-z, and 0-9) string,"
			+ " with a maximum length of 63 characters, with the '-' character allowed anywhere except the first or last character. A domain is a sequence of names "
			+ "separated by the '.' character with a maximum length of 253 characters.";
	
	private final IStatus FAILED = ValidationStatus.error(failureMessage);
	
	public LabelKeyValidator(Collection<String> readonlykeys) {
		this.readonlykeys = readonlykeys != null ? readonlykeys : new ArrayList<String>(0);
	}
	
	@Override
	public IStatus validate(Object paramObject) {
		if(!(paramObject instanceof String))
			return FAILED;
		String value= (String) paramObject;
		if(StringUtils.isEmpty(value))
			return FAILED;
		if(readonlykeys.contains(value)) {
			return ValidationStatus.error("Adding a label with a key that is the same as a readonly label is not allowed");
		}
		String [] parts = value.split("/");
		switch(parts.length) {
			case 1:
	            return super.validate(value);
			case 2:
				return (validateSubdomain(parts[0]) && validateLabel(parts[1]) ) ? ValidationStatus.OK_STATUS : FAILED;
            default:
		}
		return FAILED;
	}
	
	private boolean validateSubdomain(String value) {
		if(value.length() > SUBDOMAIN_MAXLENGTH) {
			return false;
		}
		return SUBDOMAIN_REGEXP.matcher(value).matches();
	}

}
