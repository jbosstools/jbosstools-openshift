/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.databinding;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class HostNameValidator implements IValidator {

	private static final Pattern urlPattern =
			Pattern.compile("(https?://){0,1}[^\\.:0-9]+(\\.[^\\.:0-9]+)*(:[0-9]+){0,1}");

	@Override
	public IStatus validate(Object value) {
		String server = (String) value;
		if (StringUtils.isEmpty(server)) {
			return ValidationStatus.cancel("You have to provide a server to connect to.");
		}
		if (!urlPattern.matcher(server).matches()) {
			return ValidationStatus.error("You have to provide a valid server to connect to.");
		}
		return ValidationStatus.ok();

	}
}