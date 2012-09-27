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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;

/**
 * @author Xavier Coulon
 *
 */
public class NewOpenShiftExpressApplicationWizard extends OpenShiftExpressApplicationWizard {

	/**
	 * Constructor
	 */
	public NewOpenShiftExpressApplicationWizard() {
		super(false, "New OpenShift Application");
	}

	public NewOpenShiftExpressApplicationWizard(UserDelegate user) {
		super(user, null, null, false, "New OpenShift Application");
	}
}
