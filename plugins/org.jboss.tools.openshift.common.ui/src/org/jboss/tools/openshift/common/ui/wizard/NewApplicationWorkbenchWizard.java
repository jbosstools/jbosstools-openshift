/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.common.ui.wizard;

import org.eclipse.ui.INewWizard;

/**
 * A wizard to create a new OpenShift application.
 * 
 * @author Andre Dietisheim
 * 
 */
public class NewApplicationWorkbenchWizard extends AbstractApplicationWorkbenchWizard implements INewWizard {
	
	public NewApplicationWorkbenchWizard() {
		super("New OpenShift Application");
	}

	private static final String NEW_APPLICATION_WIZARD_EXTENSION = "org.jboss.tools.openshift.ui.newApplicationWizard";

	@Override
	protected String getWizardsExtensionId() {
		return NEW_APPLICATION_WIZARD_EXTENSION;
	}
}
