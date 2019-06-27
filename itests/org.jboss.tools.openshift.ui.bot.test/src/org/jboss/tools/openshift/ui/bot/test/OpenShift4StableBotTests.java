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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterWizardHandlingOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourcesOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.HandleCustomTemplateOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.LogsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.PortForwardingOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.BuilderImageApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.LabelsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.NewApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.DeploymentOS4Test;
import org.jboss.tools.openshift.ui.bot.test.common.OCBinaryLocationTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionPropertiesTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.CreateNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.OpenNewConnectionWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.RemoveConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.SetOCForNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.StoreConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.project.CreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.DeleteProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.ProjectNameValidationTest;
import org.jboss.tools.openshift.ui.bot.test.project.ResourcesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 4 Stable Tests suite</b>
 * 
 * @contributor jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({
		// General
		OCBinaryLocationTest.class, 
		SetOCForNewConnectionTest.class,

		// Connection/
		OpenNewConnectionWizardTest.class, 
		CreateNewConnectionTest.class, 
		RemoveConnectionTest.class,
		ConnectionWizardHandlingTest.class, 
		StoreConnectionTest.class, 
		ConnectionPropertiesTest.class,

		// Project
		ProjectNameValidationTest.class, 
		CreateNewProjectTest.class, 
		DeleteProjectTest.class, 
		ResourcesTest.class,

		// Application wizard handling
		OpenNewApplicationWizardTest.class, 
		NewApplicationWizardHandlingTest.class, 
		TemplateParametersTest.class,
		LabelsTest.class, 
		BuilderImageApplicationWizardHandlingTest.class,

		// Application handling
		PortForwardingOS4Test.class, 
		LogsTest.class, 
		EditResourcesOS4Test.class, 
		HandleCustomTemplateOS4Test.class,
	
		// Creation of a new application
		DeploymentOS4Test.class,

		// Advanced application testing

		// Server adapter
		ServerAdapterWizardHandlingOS4Test.class, 
		CreateServerAdapterTest.class,

})
public class OpenShift4StableBotTests extends AbstractBotTests {

}
