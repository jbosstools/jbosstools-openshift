/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.create;

import static org.junit.Assert.fail;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.eclipse.ui.browser.BrowserEditor;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.tools.openshift.reddeer.condition.AmountOfResourcesExists;
import org.jboss.tools.openshift.reddeer.condition.BrowserContainsText;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.junit.Test;

@OCBinary
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = "eap-app", template = "resources/eap70-basic-s2i-helloworld.json")
public class DeploymentTest {

	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;

	@Test
	public void testDeploymentOfApplicationCreatedFromTemplate() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();

		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.UNSPECIFIED,
				projectReq.getProjectName()), TimePeriod.NORMAL, false);

		try {
			new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.COMPLETE, projectReq.getProjectName()),
					TimePeriod.getCustom(600), true, TimePeriod.getCustom(7));
		} catch (WaitTimeoutExpiredException ex) {
			fail("There should be a successful build of an application, but there is not.");
		}

		try {
			new WaitUntil(new AmountOfResourcesExists(Resource.POD, 2, projectReq.getProjectName()), TimePeriod.getCustom(60), true,
					TimePeriod.getCustom(7));
		} catch (WaitTimeoutExpiredException ex) {
			fail("There should be precisely 2 pods. One of the build and one of an running application.");
		}

		explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).getOpenShiftResources(Resource.ROUTE).get(0).select();
		new ContextMenu(OpenShiftLabel.ContextMenu.SHOW_IN_WEB_BROWSER).select();

		try {
			new WaitUntil(new BrowserContainsText("Hello World!"), TimePeriod.VERY_LONG);
			new BrowserEditor("helloworld").close();
		} catch (WaitTimeoutExpiredException ex) {
			fail("Application was not deployed successfully because it is not shown in web browser properly.\n"
					+ ex.getMessage());
		}
	}
}
