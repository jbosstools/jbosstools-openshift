/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.cdk.server.ui.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK32Server;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class CDK32ServerWizardFragment extends CDK3ServerWizardFragment {
	protected String profileName;
	protected Text profileText;

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing Red Hat Container Development Kit Version 3.2+";
		String label = "Minishift Binary: ";
		return createComposite(parent, handle, title, desc, label);
	}

	@Override
	protected Composite createComposite(Composite parent, IWizardHandle handle, String title, String desc,
			String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createCredentialWidgets(main);
		createHypervisorWidgets(main);
		createLocationWidgets(main, homeLabel);
		createProfileWidgets(main);

		validateAndPack(main);
		return main;
	}

	protected void createProfileWidgets(Composite main) {
		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText("Minishift Profile:");
		GridData profileData = new GridData();
		profileData.grabExcessHorizontalSpace = true;
		profileData.horizontalAlignment = SWT.FILL;
		profileData.horizontalSpan = 2;
		profileText = new Text(main, SWT.BORDER | SWT.SINGLE);
		profileText.setLayoutData(profileData);
		profileText.setText(CDK32Server.MINISHIFT_DEFAULT_PROFILE);
		profileName = CDK32Server.MINISHIFT_DEFAULT_PROFILE;

		profileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				profileName = profileText.getText();
				validate();
			}
		});
	}

	protected String isVersionCompatible(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if (cdkVers == null) {
			return "Cannot determine CDK version.";
		}
		if (CDK32Server.matchesCDK32(cdkVers)) {
			return null;
		}
		return "CDK version " + cdkVers + " is not compatible with this server adapter.";
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		super.performFinish(monitor);
		IServer s = getServerFromTaskModel();
		if (s instanceof IServerWorkingCopy) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			if (profileName != null && !profileName.isEmpty()) {
				swc.setAttribute(CDK32Server.PROFILE_ID, profileName);
			}
		}
	}
}
