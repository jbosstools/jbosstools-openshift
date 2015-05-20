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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * A wizard to create a new OpenShift application.
 * 
 * @author Andre Dietisheim
 * 
 */
public class ImportApplicationWorkbenchWizard extends AbstractApplicationWorkbenchWizard implements IImportWizard {
	
	private static final String IMPORT_APPLICATION_WIZARD_EXTENSION = "org.jboss.tools.openshift.ui.importApplicationWizard";

	@Override
	protected String getWizardsExtensionId() {
		return IMPORT_APPLICATION_WIZARD_EXTENSION;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

}
