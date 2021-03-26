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
public class CountryCodeValidator implements IValidator<String> {
  
  public static final CountryCodeValidator INSTANCE = new CountryCodeValidator();
  
  /*
   * see https://github.com/codeready-toolchain/registration-service/blob/master/pkg/assets/landingpage.js
   */
  private static final Pattern pattern = Pattern
      .compile("^[\\+]?[0-9]+$");

  @Override
  public IStatus validate(String countryCode) {
    if (StringUtils.isEmpty(countryCode)) {
      return ValidationStatus.cancel("Please provide a country code");
    } else if (!pattern.matcher(countryCode).matches()) {
      return ValidationStatus.error("Country code should be digits with an optional leading +");
    }
    return ValidationStatus.ok();
  }

 

}
