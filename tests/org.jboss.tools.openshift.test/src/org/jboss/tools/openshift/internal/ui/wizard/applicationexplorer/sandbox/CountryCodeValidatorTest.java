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
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.internal.ui.validator.CountryCodeValidator;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Red Hat Developers
 *
 */
public class CountryCodeValidatorTest {
  @Test
  public void checkCountryCodeWithoutLeadingPlus() {
    IStatus status = CountryCodeValidator.INSTANCE.validate("33");
    assertTrue(status.isOK());
  }

  @Test
  public void checkCountryCodeWithLeadingPlus() {
    IStatus status = CountryCodeValidator.INSTANCE.validate("+33");
    assertTrue(status.isOK());
  }

  @Test
  public void checkCountryCodeWithCharacters() {
    IStatus status = CountryCodeValidator.INSTANCE.validate("+xx");
    assertFalse(status.isOK());
  }

  @Test
  public void checkCountryCodeWithSpaces() {
    IStatus status = CountryCodeValidator.INSTANCE.validate("+3 3");
    assertFalse(status.isOK());
  }
}
