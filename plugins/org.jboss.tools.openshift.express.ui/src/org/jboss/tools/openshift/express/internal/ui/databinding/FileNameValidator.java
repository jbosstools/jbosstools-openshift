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

import java.io.File;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Validates a given String positively if it is non-empty and has no separator
 * chars (it's a file name, not a path)
 * 
 * @author Andre Dietisheim
 */
public class FileNameValidator extends RequiredStringValidator implements IValidator {

	public FileNameValidator() {
		super("private key file name");
	}

	@Override
	public IStatus validateString(String value) {
		if (value.indexOf(File.separator) >= 0) {
			ValidationStatus.error("You may only provide a file name, not a path.");
		}
		return ValidationStatus.ok();
	}
}
