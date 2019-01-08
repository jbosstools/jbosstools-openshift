/*******************************************************************************
 * Copyright (c) 2015-2019 Red Hat, Inc.
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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.PublishChangesTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterFromResourceTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.SpringBootTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.CreateResourcesTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.DeleteResourceTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourceLimitsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourcesTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.HandleCustomTemplateTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ImportApplicationTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.InteligentDeleteResourceTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.LogsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.OSExplorerResourceTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.PortForwardingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ScalingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.TriggerBuildTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.BuilderImageApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.LabelsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.NewApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardWithNoProjectTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationFromTemplateTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationOnBuilderImageTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.DeploymentTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.debug.DebuggingEAPAppTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.debug.NodeJSAppDebugTest;
import org.jboss.tools.openshift.ui.bot.test.common.OCBinaryLocationTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.CreateNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.OpenNewConnectionWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.RemoveConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.SetOCForNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.StoreConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.integration.docker.DeployDockerImageTest;
import org.jboss.tools.openshift.ui.bot.test.integration.docker.DeployVariousDockerImagesTest;
import org.jboss.tools.openshift.ui.bot.test.integration.openshift.io.GetOpenShiftIOTokenTest;
import org.jboss.tools.openshift.ui.bot.test.project.CreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.DeleteProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.LinkToCreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectNameValidationTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.project.ResourcesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 3 All Tests suite</b>
 * 
 * @author mlabuda@redhat.com
 * @contributor jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({
	// General
	OCBinaryLocationTest.class,
	SetOCForNewConnectionTest.class,
	
	// Connection
	OpenNewConnectionWizardTest.class,
	CreateNewConnectionTest.class,
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

	// Application wizard handling
	OpenNewApplicationWizardTest.class,
	OpenNewApplicationWizardWithNoProjectTest.class,
	NewApplicationWizardHandlingTest.class,
	TemplateParametersTest.class,
	LabelsTest.class,
	BuilderImageApplicationWizardHandlingTest.class,
	
	// Creation of a new application
	CreateApplicationFromTemplateTest.class,
	CreateApplicationOnBuilderImageTest.class, 
	DeploymentTest.class,
	
	// Application handling
	DeleteResourceTest.class, 
	InteligentDeleteResourceTest.class,
	OSExplorerResourceTest.class,
	TriggerBuildTest.class, 
	ImportApplicationTest.class,
	PortForwardingTest.class, 
	LogsTest.class, 
	EditResourcesTest.class,
	HandleCustomTemplateTest.class,
	ImportApplicationWizardTest.class,
	ImportApplicationWizardGitTest.class,

	// Advanced application testing
	DeployDockerImageTest.class,
	DeployVariousDockerImagesTest.class,
 	CreateResourcesTest.class,
	ScalingTest.class,
	EditResourceLimitsTest.class,

	// Server adapter
	ServerAdapterWizardHandlingTest.class,
	CreateServerAdapterTest.class,
	ServerAdapterFromResourceTest.class,
	PublishChangesTest.class,
	DebuggingEAPAppTest.class,
	NodeJSAppDebugTest.class,
	SpringBootTest.class,
	
	//OpenShift.io
	GetOpenShiftIOTokenTest.class
})
public class OpenShift3BotTests extends AbstractBotTests{
	
	
}
