/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
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
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK32IntegrationTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.openshift.CDKImageRegistryUrlDiscoveryFailureTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.openshift.CDKImageRegistryUrlDiscoveryTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.openshift.CDKImageRegistryUrlValidatorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK32ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK3ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.MinishiftServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.launch.CDKLaunchConfigurationTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK32ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK3ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.MinishiftServerWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	CDK3ServerWizardTest.class,
	CDK32ServerWizardTest.class,
	MinishiftServerWizardTest.class,
	CDK3ServerEditorTest.class,
	CDK32ServerEditorTest.class,
	MinishiftServerEditorTest.class,
	CDKLaunchConfigurationTest.class,
	CDKImageRegistryUrlDiscoveryTest.class,
	CDKImageRegistryUrlDiscoveryFailureTest.class,
	CDKImageRegistryUrlValidatorTest.class,
	CDK32IntegrationTest.class
})
/**
 * @author ondrej dockal
 */
public class CDKAllTestsSuite {

}
