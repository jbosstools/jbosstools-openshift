/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard;

import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * 
 * Create component wizard implemented with RedDeer(for OpenShift Application Explorer).
 * 
 * @author jkopriva@redhat.com
 *
 */
public class CreateComponentWizard extends WizardDialog {
	
	public CreateComponentWizard() {
		super(OpenShiftLabel.Shell.CREATE_COMPONENT);
	}
	
}