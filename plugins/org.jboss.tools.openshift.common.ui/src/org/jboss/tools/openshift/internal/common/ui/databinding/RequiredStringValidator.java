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
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * A validator that handles empty strings as invalid. Non-empty strings are
 * valid.
 * <p>
 * Invalid states are reported via ValidationStatus.cancel("message") so that
 * {@link RequiredControlDecorationUpdater} may decorate in custom way.
 * 
 * @author Andre Dietisheim
 * 
 * @see RequiredControlDecorationUpdater
 */
public class RequiredStringValidator implements IValidator {

    private String fieldName;

    public RequiredStringValidator(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public IStatus validate(Object value) {
        String string = (String)value;
        if (StringUtils.isEmpty(string)) {
            return ValidationStatus.cancel(NLS.bind("Please provide a {0}", fieldName));
        }
        return validateString((String)value);
    }

    public IStatus validateString(String value) {
        return ValidationStatus.ok();
    }

    protected String getFieldName() {
        return fieldName;
    }

}
