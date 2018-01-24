/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.Minishift17Server;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class Minishift17ServerWizardFragment extends CDK32ServerWizardFragment {

	@Override
	protected boolean shouldCreateCredentialWidgets() {
		return false;
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Minishift 1.7+ Server Adapter ";
		String desc = "A server adapter representing Minishift 1.7+";
		String label = "Minishift Binary: ";
		return createComposite(parent, handle, title, desc, label);
	}

	@Override
	protected String isVersionCompatible(MinishiftVersions versions) {
		return isMinishiftVersionCompatible(versions);
	}

	public static String isMinishiftVersionCompatible(MinishiftVersions versions) {
		if( versions.getCDKVersion() != null ) {
			return versions.getCDKVersion() + " is not compatible with this server adapter.";
		}

		String msVers = versions.getMinishiftVersion();
		if (msVers != null) {
			if (Minishift17Server.matchesMinishift17OrGreater(msVers)) {
				return null;
			}
			return "Minishift version " + msVers + " is not compatible with this server adapter.";
		}
		return "Cannot determine Minishift version.";
	}

}
