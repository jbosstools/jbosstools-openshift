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
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK32ServerAdapterConnectionTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK32ServerAdapterRestartTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK32ServerAdapterStartTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK32ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK3ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDKServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.launch.CDKLaunchConfigurationTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK32ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK3ServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDKServerWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	CDKServerWizardTest.class,
	CDK3ServerWizardTest.class,
	CDK32ServerWizardTest.class,
	CDKServerEditorTest.class,
	CDK3ServerEditorTest.class,
	CDK32ServerEditorTest.class,
	CDKLaunchConfigurationTest.class,
	CDK32ServerAdapterStartTest.class,
	CDK32ServerAdapterRestartTest.class,
	CDK32ServerAdapterConnectionTest.class
})
/**
 * @author ondrej dockal
 */
public class CDKAllTestsSuite {

}
