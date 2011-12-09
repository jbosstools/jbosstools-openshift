/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;

public class PassphraseDialog extends InputDialog {

	public PassphraseDialog(Shell shell) {
		super(shell
				, "New ssh key"
				, "Please pick a passphrase for your new ssh key pair"
				, null
				, new PassphraseValidator());
	}

	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER | SWT.PASSWORD;
	}

	private static class PassphraseValidator implements IInputValidator {

		@Override
		public String isValid(String input) {
			if (StringUtils.isEmpty(input)) {
				return "You have to provide a pass phrase";
			}
			return null;
		}
	}
}
