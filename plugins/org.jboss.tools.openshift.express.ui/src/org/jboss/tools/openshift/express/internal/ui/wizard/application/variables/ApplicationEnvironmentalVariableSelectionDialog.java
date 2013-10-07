/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 *
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application.variables;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.OpenShiftApplicationWizardModel;

/**
 * @author Martes G Wigglesworth
 *
 */
public class ApplicationEnvironmentalVariableSelectionDialog extends TitleAreaDialog {

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableSelectionDialog
	 * @param parentShell
	 */
	public ApplicationEnvironmentalVariableSelectionDialog(Shell parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Constructs a new instance of ApplicationEnvironmentalVariableSelectionDialog
	 * @param wizard
	 * @param wizardModel
	 * @param object
	 * @param shell
	 */
	public ApplicationEnvironmentalVariableSelectionDialog(OpenShiftApplicationWizard wizard,
			OpenShiftApplicationWizardModel wizardModel, Object object, Shell shell) {
		// TODO - Still need to resolve the lack of a wizard model within this package.
		super(wizard,wizardModel,object,shell);
	}

}
