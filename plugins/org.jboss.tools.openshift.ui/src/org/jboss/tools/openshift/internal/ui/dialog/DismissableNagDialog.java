/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog to display info about a list of resources
 * @author jeff.cantrill
 */
public class DismissableNagDialog extends MessageDialog implements IDialogConstants {

	public static final int ALWAYS = 0;
	public static final int NO = 1;
	public static final int YES = 2;
	private static final String [] LABELS = new String []{"Always Replace", NO_LABEL, YES_LABEL}; 
	
	public AtomicBoolean isOpen = new AtomicBoolean(false);
	
	public DismissableNagDialog(Shell parentShell, String dialogTitle, String dialogMessage) {
		super(parentShell, dialogTitle, null, dialogMessage, QUESTION, LABELS, YES);
	}

	@Override
	public int open() {
		try {
			isOpen.set(true);
			return super.open();
		}finally {
			isOpen.set(false);
		}
	}
	
	public boolean isOpen() {
		return isOpen.get();
	}
	
}
