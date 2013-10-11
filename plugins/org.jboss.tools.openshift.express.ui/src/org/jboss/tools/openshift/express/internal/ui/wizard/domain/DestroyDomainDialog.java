/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.common.ui.dialog.CheckboxMessageDialog;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class DestroyDomainDialog extends CheckboxMessageDialog {

	private int returnCode = Dialog.CANCEL;
	
	public DestroyDomainDialog(IDomain domain, Shell parentShell) {
		super(parentShell, "Domain deletion"
				, NLS.bind("You are about to delete the \"{0}\" domain.\nDo you want to continue?", domain.getId())
				, "Force applications deletion (data will be lost and operation cannot be undone)");
	}
	
	public boolean isCancel() {
		return (returnCode & CANCEL) != 0;
	}

	public boolean isForceDelete() {
		return (returnCode & CHECKBOX_SELECTED) != 0;
	}

	@Override
	public int open() {
		return returnCode = super.open();
	}

	
	
}
