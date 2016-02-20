/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validate project name same as Kubernetes server for namespaces
 * @author jeff.cantrill
 */
//TODO Refactor to merge base into ResourceNameValidator
public class ProjectNameValidator extends LabelValueValidator {

	public static final String projectNameDescription =
			"A valid project name must be 63 characters or less but at least 2 characters, excluding \"..\".\n"
			+ "It may only contain lower-case letters, numbers, and dashes. It may not start or end with a dash.";

	private Pattern PROJECT_NAME_PATTERN = Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");

	private String message;

	private Collection<String> unavailableNames;

	public ProjectNameValidator(String defaultMessage, Collection<String> unavailableNames) {
		super("project name");
		this.message = defaultMessage;
		this.unavailableNames = unavailableNames;
	}
	
	@Override
	public IStatus validate(Object value) {
		if(!(value instanceof String)) {
			return ValidationStatus.error(getValueIsNotAStringMessage());
		}
		String param = (String) value;
		if(StringUtils.isEmpty(param)) {
			return ValidationStatus.cancel(message);
		}
		if("..".equals(param) || ".".equals(param)) {
			return ValidationStatus.error("Project name cannot be '.' or '..'");
		}
		if(param.length() < 2) {
			return ValidationStatus.error(NLS.bind("Project name length must be between {0} and {1} characters", 2, LABEL_MAXLENGTH));
		}

		if (!PROJECT_NAME_PATTERN.matcher(param).matches()) {
			return ValidationStatus.error("Project name may only contain lower-case letters, numbers, and dashes. It may not start or end with a dash");
		}
		if (unavailableNames != null && unavailableNames.contains(param)) {
			return ValidationStatus.error("A project with the same name already exists");
		}
		return super.validate(value);
	}

}