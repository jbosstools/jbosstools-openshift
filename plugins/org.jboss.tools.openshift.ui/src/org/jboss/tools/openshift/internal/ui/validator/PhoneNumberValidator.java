/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.validator;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * @author Red Hat Developers
 *
 */
public class PhoneNumberValidator implements IValidator<String> {
  
  public static final PhoneNumberValidator INSTANCE = new PhoneNumberValidator();
  
  /*
   * see https://github.com/codeready-toolchain/registration-service/blob/master/pkg/assets/landingpage.js
   */
  private static final Pattern pattern = Pattern
      .compile("^[(]?[0-9]+[)]?[-\\s\\.]?[0-9]+[-\\s\\.\\/0-9]*$", Pattern.CASE_INSENSITIVE);

  @Override
  public IStatus validate(String phoneNumber) {
    if (StringUtils.isEmpty(phoneNumber)) {
      return ValidationStatus.cancel("Please provide a phone number");
    } else if (!pattern.matcher(phoneNumber).matches()) {
      return ValidationStatus.error("Phone number should be digits optionally separated by - or ' '");
    }
    return ValidationStatus.ok();
  }

 

}
