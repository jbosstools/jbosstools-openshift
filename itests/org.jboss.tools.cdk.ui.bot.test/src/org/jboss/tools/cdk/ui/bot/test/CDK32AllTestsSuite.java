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
import org.jboss.tools.cdk.ui.bot.test.server.editor.launch.CDKLaunchConfigurationTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK32ServerWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	CDK32ServerWizardTest.class,
	CDK32ServerEditorTest.class,
	CDKLaunchConfigurationTest.class,
	CDKImageRegistryUrlValidatorTest.class,
	CDKImageRegistryUrlDiscoveryFailureTest.class,
	CDKImageRegistryUrlDiscoveryTest.class,
	CDK32IntegrationTest.class
})
/**
 * @author ondrej dockal
 */
public class CDK32AllTestsSuite {

}
