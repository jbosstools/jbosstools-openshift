/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.databinding;

import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Numeric validator.
 * 
 */
public class NumericValidator implements IValidator {
    private String type;
    private Function<String, Number> validator;
	private boolean errorOnEmpty = false;

    public NumericValidator(String type, Function<String, Number> validator, boolean errorOnEmpty) {
        this.type = type;
        this.validator = validator;
    	this.errorOnEmpty = errorOnEmpty;
    }

    public NumericValidator(String type, Function<String, Number> validator) {
    	this(type, validator, false);
    }

    @Override
    public IStatus validate(Object value) {
        if (!(value instanceof String)) {
            return ValidationStatus.error("Invalid format");
        }
        String str = (String) value;
        IStatus status = ValidationStatus.ok();
        if (StringUtils.isEmpty(str)) {
        	if (errorOnEmpty) {
        		status = ValidationStatus.error("Please provide a value");
        	}
        } else {
        	try {
                validator.apply(str);
            }
            catch (NumberFormatException e) {
                status = ValidationStatus.error("Must be an " + type);
            }
        }
        return status;
    }
}