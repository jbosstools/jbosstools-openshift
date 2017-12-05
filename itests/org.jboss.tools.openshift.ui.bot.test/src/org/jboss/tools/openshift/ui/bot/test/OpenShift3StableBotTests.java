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

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.CreateServerAdapterTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ImportApplicationWizardGitTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ImportApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.CreateResourcesTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourceLimitsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.LabelsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.NewApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.common.OCBinaryLocationTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.OpenNewConnectionWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.RemoveConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.StoreConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.integration.docker.DeployDockerImageTest;
import org.jboss.tools.openshift.ui.bot.test.project.CreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.DeleteProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.LinkToCreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectNameValidationTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.project.ResourcesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 3 Stable Tests suite</b>
 * 
 * @author jkopriva@redhat.com
 * 
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({	
	OCBinaryLocationTest.class,
	
	// Connection
	OpenNewConnectionWizardTest.class,
	RemoveConnectionTest.class,
	ConnectionWizardHandlingTest.class,
	StoreConnectionTest.class,
	ConnectionPropertiesTest.class,
	
	// Project
	ProjectNameValidationTest.class,
	LinkToCreateNewProjectTest.class,
	CreateNewProjectTest.class,
	DeleteProjectTest.class,
	ResourcesTest.class,
	ProjectPropertiesTest.class,
	
	// Advanced application testing
	DeployDockerImageTest.class,
	EditResourceLimitsTest.class,
	
	// Application wizard handling
	NewApplicationWizardHandlingTest.class,
	TemplateParametersTest.class,
	LabelsTest.class,
	
	// Application handling
	ImportApplicationWizardTest.class,
	ImportApplicationWizardGitTest.class,
	CreateResourcesTest.class,
	
	// Server adapter
	ServerAdapterWizardHandlingTest.class,
	CreateServerAdapterTest.class,
	
})
public class OpenShift3StableBotTests extends AbstractBotTests {

}
