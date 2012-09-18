package org.jboss.tools.openshift.express.internal.ui.wizard.ssh;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.openshift.express.internal.ui.databinding.AlphanumericStringValidator;

public class SSHPublicKeyNameValidator extends AlphanumericStringValidator {

	private AddSSHKeyWizardPageModel model;

	public SSHPublicKeyNameValidator(AddSSHKeyWizardPageModel model) {
		super("key name");
		this.model = model;
	}

	@Override
	public IStatus validate(Object value) {
		IStatus validationStatus = super.validate(value);
		if (!validationStatus.isOK()) {
			return validationStatus;
		}
		String keyName = (String) value;
		if (model.hasKeyName(keyName)) {
			return ValidationStatus.error("There's already a key with the name " + keyName);
		}
		return ValidationStatus.ok();

	}
}
