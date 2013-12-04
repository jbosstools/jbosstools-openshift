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

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Validates a given passphrase confirmation value such that it matches the 
 * model's passphrase value
 */
public class PassPhraseConfirmValidator extends MultiValidator {

	private IObservableValue passphrase;
	private IObservableValue passphraseConfirm;

	public PassPhraseConfirmValidator(IObservableValue passphrase,
			IObservableValue passphraseConfirm) {
		this.passphrase = passphrase;
		this.passphraseConfirm = passphraseConfirm;
	}

	@Override
	protected IStatus validate() {
		Object o1 = passphrase.getValue();
		Object o2 = passphraseConfirm.getValue();
		boolean bothEmpty = (o1 == null || "".equals(o1)) && (o2 == null || "".equals(o2));
		if( !bothEmpty ) {
			// At least one is not null.  if non-null object NOT EQUAL possibly-null object, show error
			if( !(o1 == null ? o2 : o1).equals( o1 == null ? o1 :o2)) 
				return ValidationStatus.error("Please ensure the two passphrases match.");
		}
		return ValidationStatus.ok();
	}

	@Override
	public IObservableList getTargets() {
		WritableList targets = new WritableList();
		targets.add(passphraseConfirm);
		return targets;
	}
}
