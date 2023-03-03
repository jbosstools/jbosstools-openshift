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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.PublishChangesOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterWizardHandlingOS4Test;
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
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.BuilderImageApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.LabelsTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.NewApplicationWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationFromTemplateOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationOnBuilderImageTest;
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
import org.jboss.tools.openshift.ui.bot.test.project.LinkToCreateNewProjectTest;
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
		// not yet stable is added in commented line to easily track not stable tests
	
		// General
//		OCBinaryLocationTest.class, // pass
//		SetOCForNewConnectionTest.class, // pass

		// Connection/
//		OpenNewConnectionWizardTest.class, // pass
//		CreateNewConnectionTest.class, // pass
//		RemoveConnectionTest.class,// pass
//		ConnectionWizardHandlingTest.class, // pass
//		StoreConnectionTest.class, // failed
//		ConnectionPropertiesTest.class, // pass

		// Project
//		ProjectNameValidationTest.class, // pass
//		LinkToCreateNewProjectTest.class, // pass
//		CreateNewProjectTest.class, // pass
//		DeleteProjectTest.class, // pass
//		ResourcesTest.class, //  failed
		// not yet stable ProjectPropertiesTest.class,

		// Application wizard handling
//		OpenNewApplicationWizardTest.class,  // pass
	 	// not yet stable OpenNewApplicationWizardWithNoProjectTest.class,
//		NewApplicationWizardHandlingTest.class, // pass 
		TemplateParametersTest.class, // EAP, pass  
//		LabelsTest.class, // pass
//		BuilderImageApplicationWizardHandlingTest.class, // pass

		// Application handling
//		DeleteResourceOS4Test.class, // blocked
//		InteligentDeleteResourceOS4Test.class, // blocked
//		OSExplorerResourceTest.class, //  pass
//		TriggerBuildOS4Test.class, // failed - cannot use properties
		ImportApplicationOS4Test.class, // EAP, pass
//		PortForwardingOS4Test.class,  // failed - cannot use properties
//		LogsTest.class, // failed
//		EditResourcesOS4Test.class, // pass
//		HandleCustomTemplateOS4Test.class, // pass
//		ImportApplicationWizardTest.class, // pass
//		ImportApplicationWizardGitTest.class, // pass	
	
		// Creation of a new application
		CreateApplicationFromTemplateOS4Test.class, //  EAP, pass
//		CreateApplicationOnBuilderImageTest.class, // pass
//		DeploymentOS4Test.class, // EAP, pass	

		// Advanced application testing
//		CreateResourcesOS4Test.class, // pass
//		ScalingOS4Test.class, // failed - blocked
//		EditResourceLimitsOS4Test.class, // pass 

		// Server adapter
		ServerAdapterWizardHandlingOS4Test.class, // EAP, pass
//		CreateServerAdapterTest.class, // not sure
		// not yet stable ServerAdapterFromResourceOS4Test.class,
//		PublishChangesOS4Test.class,
		//not yet stable SpringBootTest.class,

})
public class OpenShift4StableBotTests extends AbstractBotTests {

}
