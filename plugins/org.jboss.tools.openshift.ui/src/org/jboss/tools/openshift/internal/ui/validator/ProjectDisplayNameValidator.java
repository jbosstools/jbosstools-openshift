package org.jboss.tools.openshift.internal.ui.validator;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;

public class ProjectDisplayNameValidator implements IValidator {
	
	public static final int DISPLAY_NAME_LENGTH_LIMIT = 65535;

	@Override
	public IStatus validate(Object value) {
		if(value != null && !(value instanceof String))
			return ValidationStatus.cancel("Display name is not an instance of a string");
		String param = (String) value;
		if(StringUtils.isNotEmpty(param)) {
			if(param.indexOf(SWT.TAB) >= 0 || param.indexOf(SWT.LF) >= 0) {
				return ValidationStatus.error("Display name may not contain tabs or new lines");
			}
			if (param.length() > DISPLAY_NAME_LENGTH_LIMIT) {
				return ValidationStatus.error("Display name may not be longer than " + DISPLAY_NAME_LENGTH_LIMIT + " characters");
			}
		}
		return ValidationStatus.ok();
	}

}
