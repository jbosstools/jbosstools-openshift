/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.reddeer.condition.AmountOfResourcesExists;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.junit.Test;


@OCBinary
@RequiredBasicConnection
@RequiredProject
@RequiredService(service = "eap-app", template = "resources/eap70-basic-s2i-helloworld.json")
public class TriggerBuildTest {

	@InjectRequirement
	private static OpenShiftProjectRequirement projectReq;

	
	@Test
	public void testCreateNewBuildFromBuildConfig() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD_CONFIG, (Matcher<String>) null, ResourceState.UNSPECIFIED, projectReq.getProjectName()), 
				TimePeriod.getCustom(120), true);
		
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, "eap-app-1", ResourceState.UNSPECIFIED, projectReq.getProjectName()), 
				TimePeriod.LONG, true);
		
		List<OpenShiftResource> builds = explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).
				getOpenShiftResources(Resource.BUILD);
		int oldAmountOfBuilds = builds.size();
		
		explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).getOpenShiftResources(Resource.BUILD_CONFIG).get(0).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.START_BUILD).select();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		try {
			new WaitUntil(new AmountOfResourcesExists(Resource.BUILD, oldAmountOfBuilds + 1, projectReq.getProjectName()), TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("New build was not triggered altough it should be.");
		}
	}
	
	@Test
	public void testCloneExistingBuild() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();
		
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, (Matcher<String>) null, ResourceState.UNSPECIFIED, projectReq.getProjectName()), 
					TimePeriod.getCustom(240), true);
		
		List<OpenShiftResource> builds = explorer.getOpenShift3Connection().getProject(projectReq.getProjectName()).
				getOpenShiftResources(Resource.BUILD);
		builds.get(0).select();
		new ContextMenuItem(OpenShiftLabel.ContextMenu.CLONE_BUILD).select();

		int oldAmountOfBuilds = builds.size();
		
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
		try {
			new WaitUntil(new AmountOfResourcesExists(Resource.BUILD, oldAmountOfBuilds + 1, projectReq.getProjectName()),
					TimePeriod.LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("New build was not triggered altough it should be.");
		}
	}
}
