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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasNoChange;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasText;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.hamcrest.core.StringStartsWith;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftResourceExists;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
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
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.jboss.tools.openshift.ui.bot.test.common.OpenShiftUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@OCBinary(setOCInPrefs=true)
@RequiredBasicConnection()
@CleanConnection
@RequiredProject()
@RequiredService(service=OpenShiftResources.EAP_SERVICE, template=OpenShiftResources.EAP_TEMPLATE, waitForBuild=false)
public class LogsTest extends AbstractTest {

	private static final int WAIT_CONSOLE_NO_CHANGE = 10;
	private static final int WAIT_CONSOLE_PUSH_SUCCESS = 600;

	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;

	private OpenShiftExplorerView explorer;
	private ConsoleView consoleView;
	
	
	@Before
	public void setUp() {
		assertThat(requiredService.getService(), notNullValue());
		
		this.explorer = new OpenShiftExplorerView();
		explorer.open();
		explorer.activate();
		
		this.consoleView = new ConsoleView();
		this.consoleView.open();	
	}
	

	@Test
	public void shouldShowLogFromApplicationPodContextMenu() {
		new WaitUntil(new OpenShiftResourceExists(Resource.POD, new StringStartsWith("eap-app-"), ResourceState.RUNNING, requiredProject.getProjectName(), requiredConnection.getConnection()), TimePeriod.VERY_LONG);
		
		this.consoleView = new ConsoleView();
		this.consoleView.open();
		
		OpenShiftResource pod  = OpenShiftUtils.getOpenShiftPod(requiredProject.getProjectName(),new StringStartsWith("eap-app-"), requiredConnection.getConnection());
		String podName = pod.getName();	
		waitForLog(pod, OpenShiftLabel.ContextMenu.POD_LOG);

		new WaitUntil(new ConsoleHasText(), TimePeriod.DEFAULT);
		new WaitUntil(new ConsoleHasNoChange(TimePeriod.getCustom(WAIT_CONSOLE_NO_CHANGE)), TimePeriod.VERY_LONG);

		assertTrue("Console label is incorrect, it should contains project name and pod name.\n" + "but label is: " + consoleView.getConsoleLabel(),
				consoleView.getConsoleLabel().contains(requiredProject.getProjectName() + "\\" + podName));
		assertTrue("Console text should contain output from EAP runtime. Console output:" + consoleView.getConsoleText(),
				consoleView.getConsoleText().contains("Admin console is not enabled"));
	}

	@Test
	public void shouldShowLogFromBuildPodContextMenu() {
		new WaitUntil(new OpenShiftResourceExists(Resource.BUILD, new StringStartsWith("eap-app-"), ResourceState.RUNNING, requiredProject.getProjectName(), requiredConnection.getConnection()), TimePeriod.VERY_LONG);
		
		this.consoleView = new ConsoleView();
		this.consoleView.open();
		
		OpenShiftResource pod = OpenShiftUtils.getOpenShiftPod(requiredProject.getProjectName(), Resource.BUILD, new StringStartsWith("eap-app-"), requiredConnection.getConnection());
		String podName = pod.getName();
		waitForLog(pod, OpenShiftLabel.ContextMenu.BUILD_LOG);

		new WaitUntil(new ConsoleHasText(), TimePeriod.LONG);
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
	
}
