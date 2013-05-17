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
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * @author Andre Dietisheim
 */
public class WizardUtils {

	private WizardUtils() {
	}

	public static void close(IWizard wizard) {
		IWizardContainer container = wizard.getContainer();
		if (container instanceof WizardDialog) {
			((WizardDialog) container).close();
		}
	}
	
	
}
