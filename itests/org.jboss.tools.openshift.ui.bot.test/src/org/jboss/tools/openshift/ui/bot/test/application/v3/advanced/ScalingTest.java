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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.jboss.reddeer.common.exception.WaitTimeoutExpiredException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitUntil;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.condition.WidgetIsEnabled;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.button.YesButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.spinner.DefaultSpinner;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.condition.PodsAreDeployed;
import org.jboss.tools.openshift.reddeer.enums.Resource;
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
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author mlabubda@red
 * @author adietish
 *
 */
@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection()
@RequiredProject()
@RequiredService(service=OpenShiftResources.NODEJS_SERVICE, 
template=OpenShiftResources.NODEJS_TEMPLATE)

public class ScalingTest {

	@InjectRequirement
	private OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	private OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	private OpenShiftServiceRequirement requiredService;
	private OpenShift3Connection connection;
	private OpenShiftProject project;

	@Before
	public void setUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		this.connection = explorer.getOpenShift3Connection(requiredConnection.getConnection());
		this.project = connection.getProject(requiredProject.getProjectName());
		this.project.expand();
	}

	@Test
	public void testScaleApplicationViaContextMenuOfService() {
		Service service = project.getService(OpenShiftResources.NODEJS_SERVICE);
		assertNotNull(service);

		// ensure we scale
		int pods = PodsAreDeployed.getNumberOfCurrentReplicas(project, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER);
		if (pods == 1) {
			pods = 2;
		} else {
			pods = 1;
		}

		service.select();
		scaleTo(pods);
		assertPodAmountDesiredEqualsCurrent(pods, project);

		service.select();
		scaleUp();
		assertPodAmountDesiredEqualsCurrent(++pods, project);

		service.select();
		scaleDown();
		assertPodAmountDesiredEqualsCurrent(--pods, project);
	}

	@Test
	public void testScaleApplicationViaContextMenuOfReplicationController() {
		OpenShiftResource replicationController = 
				project.getOpenShiftResource(Resource.DEPLOYMENT, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER);
		assertNotNull(replicationController);
		
		// ensure we scale
		int pods = PodsAreDeployed.getNumberOfCurrentReplicas(project, replicationController.getName());
		if (pods == 1) {
			pods = 2;
		} else {
			pods = 1;
		}

		replicationController.select();
		scaleTo(pods);
		assertPodAmountDesiredEqualsCurrent(pods, project);

		replicationController.select();
		scaleUp();
		assertPodAmountDesiredEqualsCurrent(++pods, project);

		replicationController.select();
		scaleDown();
		assertPodAmountDesiredEqualsCurrent(--pods, project);
	}
	
	@Test
	public void scaleTo0ShouldWarn() {
		int pods = requiredService.getReplicationController().getCurrentReplicaCount();
		new WaitUntil(
				new PodsAreDeployed(project, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER, pods),
				TimePeriod.LONG);

		OpenShiftResource replicationController = 
				project.getOpenShiftResource(Resource.DEPLOYMENT, OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER);
		replicationController.select();
		scaleTo(0);
		new DefaultShell(OpenShiftLabel.Shell.STOP_ALL_DEPLOYMENTS).setFocus();
		new YesButton().click();
		
		assertPodAmountDesiredEqualsCurrent(0, project);
	}

	private void scaleUp() {
		new ContextMenu(OpenShiftLabel.ContextMenu.SCALE_UP).select();
	}

	private void scaleDown() {
		new ContextMenu(OpenShiftLabel.ContextMenu.SCALE_DOWN).select();
	}

	private void scaleTo(int amountOfPods) {
		new ContextMenu(OpenShiftLabel.ContextMenu.SCALE_TO).select();
		new WaitUntil(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.SCALE_DEPLOYMENTS));
		new DefaultShell(OpenShiftLabel.Shell.SCALE_DEPLOYMENTS).setFocus();
		new WaitWhile(new WidgetIsEnabled(new OkButton()));
		new DefaultSpinner().setValue(amountOfPods);
		new WaitUntil(new WidgetIsEnabled(new OkButton()));
		new OkButton().click();
	}

	private void assertPodAmountDesiredEqualsCurrent(int podAmount, OpenShiftProject project) {
		assertPodAmountDesiredEqualsCurrent(podAmount, 
				OpenShiftResources.NODEJS_APP_REPLICATION_CONTROLLER, project);
	}

	private void assertPodAmountDesiredEqualsCurrent(int podAmount, String rcName, OpenShiftProject project) {
		try {
			new WaitUntil(
					new PodsAreDeployed(project, rcName, podAmount), TimePeriod.VERY_LONG);
		} catch (WaitTimeoutExpiredException ex) {
			fail("Pods have not been scaled, amount of current and desired pods do " 
					+ "not match. Desired amount:" + podAmount 
					+ ". Real amount of replicas: " + PodsAreDeployed.getReplicasInfo(project, rcName) +
					". Could be failing because of https://issues.jboss.org/browse/JBIDE-23638");
		}
	}
}
