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
package org.jboss.tools.openshift.ui.bot.test.application.v3.advanced;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.jboss.reddeer.eclipse.condition.ConsoleHasText;
import org.jboss.reddeer.eclipse.ui.console.ConsoleView;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.ConsoleHasSomeText;
import org.jboss.tools.openshift.reddeer.condition.PodsAreDeployed;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@OCBinary
@RequiredBasicConnection()
@CleanConnection
@RequiredProject()
@RequiredService(service=OpenShiftResources.EAP_SERVICE, template=OpenShiftResources.EAP_TEMPLATE)
public class LogsTest {

	private static final String  PODS_TAB_BUILD_POD_POSTFIX = "build";
	private static final int WAIT_CONSOLE_NO_CHANGE = 7;
	private static final int WAIT_CONSOLE_PUSH_SUCCESS = 600;

	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;

	private OpenShiftExplorerView explorer;
	private ConsoleView consoleView;
	private String serviceName;
	
	@Before
	public void setUp() {
		assertThat(requiredService.getService(), notNullValue());
		this.serviceName = requiredService.getService().getName();

		this.explorer = new OpenShiftExplorerView();
		explorer.open();
		explorer.activate();
		
		OpenShift3Connection os3Connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());		
		new WaitUntil(new PodsAreDeployed(os3Connection.getProject(requiredProject.getProjectName()),
				requiredService.getReplicationController().getName(), 1));
		
		this.consoleView = new ConsoleView();
		this.consoleView.open();		
	}

	@Test
	public void shouldShowLogFromApplicationPodContextMenu() {
		OpenShiftResource pod  = getApplicationPod(serviceName, getOpenShiftProject(requiredProject.getProjectName(), explorer));
		String podName = pod.getName();
		pod.select();
		new ContextMenu(OpenShiftLabel.ContextMenu.POD_LOG).select();

		new WaitUntil(new ConsoleHasSomeText(), TimePeriod.NORMAL);
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.getCustom(WAIT_CONSOLE_NO_CHANGE)), TimePeriod.VERY_LONG);

		assertTrue("Console label is incorrect, it should contains project name and pod name.\n" + "but label is: " + consoleView.getConsoleLabel(),
				consoleView.getConsoleLabel().contains(requiredProject.getProjectName() + "\\" + podName));
		assertTrue("Console text should contain output from EAP runtime",
				consoleView.getConsoleText().contains("Admin console is not enabled"));
	}

	@Test
	public void shouldShowLogFromBuildPodContextMenu() {
		OpenShiftResource pod = getBuildPod(serviceName, getOpenShiftProject(requiredProject.getProjectName(), explorer));
		String podName = pod.getName();
		pod.select();
		new ContextMenu(OpenShiftLabel.ContextMenu.BUILD_LOG).select();

		new WaitUntil(new ConsoleHasSomeText(), TimePeriod.NORMAL);
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.getCustom(WAIT_CONSOLE_NO_CHANGE)), TimePeriod.VERY_LONG);

		assertTrue("Console label is incorrect, it should contain project name and name of build pod.\n"
				+ "but label is: " + consoleView.getConsoleLabel(), 
				consoleView.getConsoleLabel().contains(requiredProject.getProjectName() + "\\" + podName));
		try {
			new WaitUntil(new ConsoleHasText("Push successful"), 
					TimePeriod.getCustom(WAIT_CONSOLE_PUSH_SUCCESS));
		} catch (WaitTimeoutExpiredException ex) {
			fail("There should be output of succesful build in console log, but there is not.\n"
					+ "Check whether output has not changed. Assumed output in the end of log is 'Push successful'");
		}
	}

	private OpenShiftProject getOpenShiftProject(String projectName, OpenShiftExplorerView explorer) {
		OpenShift3Connection connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());
		OpenShiftProject project = connection.getProject(projectName);
		assertThat("Could not find project " + projectName + " in OpenShift Explorer",  project, notNullValue());
		project.expand();
		return project;
	}

	private OpenShiftResource getBuildPod(String name, OpenShiftProject project) {
		assertTrue(!StringUtils.isBlank(name));

		for (OpenShiftResource pod : project.getOpenShiftResources(Resource.BUILD)) {
			String podName = pod.getName();
			if (!StringUtils.isBlank(podName)) {
				if (podName.startsWith(name)) {
					return pod;
				}
			}
		}
		fail("Build pod for service " + serviceName + " was not found");
		return null;
	}

	private OpenShiftResource getApplicationPod(String name, OpenShiftProject project) {
		assertTrue(!StringUtils.isBlank(name));

		for (OpenShiftResource pod : project.getOpenShiftResources(Resource.POD)) {
			String podName = pod.getName();
			if (!StringUtils.isBlank(podName)) {
				if (podName.startsWith(name)
						&& !podName.endsWith(PODS_TAB_BUILD_POD_POSTFIX)) {
					return pod;
				}
			}
		}
		fail("Pod for service " + serviceName + " was not found");
		return null;
	}

}
