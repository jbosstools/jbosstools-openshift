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
import org.jboss.tools.openshift.internal.ui.validator.PhoneNumberValidator;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Red Hat Developers
 *
 */
public class PhoneNumberValidatorTest {
  @Test
  public void checkPhoneNumberWithDigits() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("0123456789");
    assertTrue(status.isOK());
  }

  @Test
  public void checkPhoneNumberWithDigitsAndSpaces() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("012 345 6789");
    assertTrue(status.isOK());
  }

  @Test
  public void checkPhoneNumberWithDigitsAndDashes() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("012-345-6789");
    assertTrue(status.isOK());
  }

  @Test
  public void checkPhoneNumberWithDigitsAndParenthesis() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("(012)-(345)-6789");
    assertFalse(status.isOK());
  }

  @Test
  public void checkPhoneNumberWithDigitsAndLeadingParenthesis() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("(012)-345-6789");
    assertTrue(status.isOK());
  }

  @Test
  public void checkPhoneNumberWithCharacters() {
    IStatus status = PhoneNumberValidator.INSTANCE.validate("01234x56789");
    assertFalse(status.isOK());
  }
}
