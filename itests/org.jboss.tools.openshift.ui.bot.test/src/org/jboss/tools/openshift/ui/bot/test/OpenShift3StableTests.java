/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.BuilderImageApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.LabelsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.NewApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.common.OCBinaryLocationTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.CreateNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.OpenNewConnectionWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.RemoveConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.project.CreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.DeleteProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.LinkToCreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectNameValidationTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.project.ResourcesTest;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 3 Stable Tests suite</b>
 * 
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({	
	// General
	OCBinaryLocationTest.class,
	
	// Connection
	OpenNewConnectionWizardTest.class,
	CreateNewConnectionTest.class,
	RemoveConnectionTest.class,
	ConnectionWizardHandlingTest.class,
	ConnectionPropertiesTest.class,
	
	// Project
	ProjectNameValidationTest.class,
	LinkToCreateNewProjectTest.class,
	CreateNewProjectTest.class,
	DeleteProjectTest.class,
	ResourcesTest.class,
	ProjectPropertiesTest.class,
	
	// Application wizard handling
	OpenNewApplicationWizardTest.class,
	NewApplicationWizardHandlingTest.class,
	TemplateParametersTest.class,
	LabelsTest.class,
	BuilderImageApplicationWizardHandlingTest.class,
})
public class OpenShift3StableTests {

	@AfterClass
	public static void cleanUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		OpenShift3Connection connection = explorer.getOpenShift3Connection();

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
		} catch (RedDeerException e) {
			// swallow intentionally
		}
	}

}
