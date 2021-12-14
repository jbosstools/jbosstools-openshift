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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ImportApplicationWizardGitTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ImportApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.CreateResourcesOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.DeleteResourceOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourceLimitsOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.EditResourcesOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.HandleCustomTemplateOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ImportApplicationOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.InteligentDeleteResourceOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.LogsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.OSExplorerResourceTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.PortForwardingOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ScalingOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.TriggerBuildOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationFromTemplateOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationOnBuilderImageTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.DeploymentOS4Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 4 Stable Tests suite</b>
 * 
 * @contributor jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({
		//not yet stable is added in commented line to easily track not stable tests
	
		// General
		/*OCBinaryLocationTest.class, 
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
		LinkToCreateNewProjectTest.class,
		CreateNewProjectTest.class, 
		DeleteProjectTest.class, 
		ResourcesTest.class,
		//not yet stable ProjectPropertiesTest.class,

		// Application wizard handling
		OpenNewApplicationWizardTest.class, 
		//not yet stable OpenNewApplicationWizardWithNoProjectTest.class,
		NewApplicationWizardHandlingTest.class, 
		TemplateParametersTest.class,
		LabelsTest.class, 
		BuilderImageApplicationWizardHandlingTest.class,

		// Application handling*/
		DeleteResourceOS4Test.class,
		InteligentDeleteResourceOS4Test.class,
		OSExplorerResourceTest.class,
		TriggerBuildOS4Test.class,
		ImportApplicationOS4Test.class,
		PortForwardingOS4Test.class, 
		LogsTest.class, 
		EditResourcesOS4Test.class, 
		HandleCustomTemplateOS4Test.class,
		ImportApplicationWizardTest.class,
		ImportApplicationWizardGitTest.class,
	
		// Creation of a new application
		CreateApplicationFromTemplateOS4Test.class,
		CreateApplicationOnBuilderImageTest.class,
		DeploymentOS4Test.class,

		// Advanced application testing
		CreateResourcesOS4Test.class,
		ScalingOS4Test.class,
		EditResourceLimitsOS4Test.class,

		// Server adapter
		//not yet stable ServerAdapterWizardHandlingOS4Test.class, 
		//not yet stable CreateServerAdapterTest.class,
		//not yet stable ServerAdapterFromResourceOS4Test.class,
		//not yet stable PublishChangesOS4Test.class,
		//not yet stable SpringBootTest.class,

})
public class OpenShift4StableBotTests extends AbstractBotTests {

}