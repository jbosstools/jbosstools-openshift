/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.cdk.server.core.adapter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.internal.cdk.server.core.CDKCoreActivator;

public class CDK32Poller extends MinishiftPoller {

	@Override
	protected void launchThread() {
		launchThread("CDK Minishift 3.2+ Poller");
	}

	@Override
	protected IStatus parseOutput(String[] lines) {
		for (int i = 0; i < lines.length; i++) {
			if (lines[i] != null && lines[i].startsWith("Minishift:")) {
				String stat = lines[i].substring("Minishift:".length()).trim();
				if ("Running".equals(stat)) {
					// throws OpenShiftNotReadyPollingException on failure
					return Status.OK_STATUS;
				}
				if ("Stopped".equals(stat) || "Does Not Exist".equals(stat)) {
					return CDKCoreActivator.statusFactory()
							.errorStatus("minishift status indicates the CDK is stopped.");
				}
			}
		}
		return StatusFactory.infoStatus(CDKCoreActivator.PLUGIN_ID,
				"minishift status indicates the CDK is starting.");
	}
}
