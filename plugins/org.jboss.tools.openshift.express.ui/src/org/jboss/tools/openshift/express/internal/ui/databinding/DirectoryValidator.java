/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.databinding;

import java.io.File;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * @author Andre Dietisheim
 */
public class DirectoryValidator extends RequiredStringValidator {

	public DirectoryValidator(String fieldName) {
		super(fieldName);
	}

	@Override
	public IStatus validateString(String value) {
		File directory = new File(value);
		if (!directory.isDirectory()) {
			return ValidationStatus.error(NLS.bind("{0} is not a directory.", value));
		}
		if (!directory.exists()) {
			return ValidationStatus.error(NLS.bind("The directory {0} does not exist.", directory));
		}
		return ValidationStatus.ok();
	}

}
