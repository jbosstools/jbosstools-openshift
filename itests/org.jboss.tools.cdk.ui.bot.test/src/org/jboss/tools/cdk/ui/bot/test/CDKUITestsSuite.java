/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDKServerAdapterSetupCDKTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK32ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK3ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.MinishiftServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.launch.CDKLaunchConfigurationTest;
import org.jboss.tools.cdk.ui.bot.test.server.runtime.CDK32RuntimeDetectionDevstudioStartUpTest;
import org.jboss.tools.cdk.ui.bot.test.server.runtime.CDK32RuntimeDetectionTest;
import org.jboss.tools.cdk.ui.bot.test.server.runtime.CDK3RuntimeDetectionTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK32ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK3ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.MinishiftServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.download.DownloadContainerRuntimeDefaultSettingsTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.download.DownloadLatestContainerRuntimeTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.download.DownloadRuntimesWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	
	// UI integration tests
	CDK32RuntimeDetectionDevstudioStartUpTest.class,
	CDK3RuntimeDetectionTest.class,
	CDK32RuntimeDetectionTest.class,
	
	// Wizard tests by server adapter type
	CDK3ServerWizardTest.class,
	CDK32ServerWizardTest.class,
	MinishiftServerWizardTest.class,
	
	// Downloading and installation of CDK/Minishift runtimes via new server wizard
	DownloadLatestContainerRuntimeTest.class,
	DownloadContainerRuntimeDefaultSettingsTest.class,
	DownloadRuntimesWizardTest.class,
	
	// Server editor tests by server adapter type
	CDK3ServerEditorTest.class,
	CDK32ServerEditorTest.class,
	MinishiftServerEditorTest.class,
	CDKLaunchConfigurationTest.class,
	
	// Setup CDK context menu item tests
	CDKServerAdapterSetupCDKTest.class,
})
/**
 * 
 * @author odockal
 *
 */
public class CDKUITestsSuite {	
}
