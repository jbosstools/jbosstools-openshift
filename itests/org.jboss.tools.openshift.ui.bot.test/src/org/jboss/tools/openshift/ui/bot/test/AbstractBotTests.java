/*******************************************************************************
 * Copyright (c) 2017-2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.junit.AfterClass;
import org.junit.runner.RunWith;

/**
 * <b>OpenShift Abstract class for Tests suites</b>
 * 
 * @author jkopriva@redhat.com
 */

@RunWith(RedDeerSuite.class)
@RequiredBasicConnection
public abstract class AbstractBotTests {


	@InjectRequirement
	private static OpenShiftConnectionRequirement connectionReq;
	
	@AfterClass
	public static void cleanUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());

		if (connection != null) {
			for (OpenShiftProject project : connection.getAllProjects()) {
				safeDeleteProject(project, connection);
			}

			new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		}
	}

	private static void safeDeleteProject(OpenShiftProject project, OpenShift3Connection connection) {
		try {
			connection.refresh();
			project.delete();
		} catch (CoreLayerException e) {
			// swallow intentionally
		}
	}

}
