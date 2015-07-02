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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validate project name same as Kubernetes server for namespaces
 * @author jeff.cantrill
 */
public class ProjectNameValidator extends LabelValueValidator {

	private String message;

	public ProjectNameValidator(String defaultMessage) {
		super("project name");
		this.message = defaultMessage;
	}
	
	@Override
	public IStatus validate(Object value) {
		if(!(value instanceof String)) {
			return getFailedStatus();
		}
		String param = (String) value;
		if(StringUtils.isEmpty(param)) {
			return ValidationStatus.cancel(message);
		}
		if("..".equals(param) || ".".equals(param)) {
			return ValidationStatus.error("Project name cannot be '.' or '..'");
		}
		if(param.length() < 2) {
			return ValidationStatus.error(NLS.bind("Project name length must be between {0} and {1} characters.", 2, LABEL_MAXLENGTH));
		}
		return super.validate(value);
	}

}