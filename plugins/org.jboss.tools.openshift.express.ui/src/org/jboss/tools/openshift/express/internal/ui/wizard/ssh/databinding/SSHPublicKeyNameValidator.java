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
package org.jboss.tools.openshift.express.internal.ui.wizard.ssh.databinding;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.express.internal.ui.databinding.AlphanumericStringValidator;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.ISSHKeyWizardPageModel;

/**
 * @author Andre Dietisheim
 */
public class SSHPublicKeyNameValidator extends AlphanumericStringValidator {

	private ISSHKeyWizardPageModel model;

	public SSHPublicKeyNameValidator(ISSHKeyWizardPageModel model) {
		super("key name");
		this.model = model;
	}

	@Override
	public IStatus validateString(String value) {
		IStatus validationStatus = super.validateString(value);
		if (!validationStatus.isOK()) {
			return validationStatus;
		}
		if (model.hasKeyName(value)) {
			return ValidationStatus.error("There's already a key with the name " + value);
		}
		return ValidationStatus.ok();

	}
}
