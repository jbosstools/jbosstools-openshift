package org.jboss.tools.openshift.express.internal.ui.wizard.ssh.databinding;

import java.io.File;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredStringValidator;

/**
 * @author Andre Dietisheim
 */
public class DirectoryValidator extends RequiredStringValidator {

	public DirectoryValidator(String fieldName) {
		super(fieldName);
	}

	@Override
	public IStatus validateString(String value) {
		File ssh2HomeDirectory = new File(value);
		if (!ssh2HomeDirectory.isDirectory()) {
			return ValidationStatus.error(value + " is not a directory.");
		}
		if (!ssh2HomeDirectory.exists()) {
			return ValidationStatus.error("The directory " + value + " does not exist.");
		}
		return ValidationStatus.ok();
	}

}
