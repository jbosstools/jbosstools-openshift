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
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK3ServerAdapterConnectionTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK3ServerAdapterRestartTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CDK3ServerAdapterStartTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CDK3ServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CDK3ServerWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	CDK3ServerWizardTest.class,
	CDK3ServerEditorTest.class,
	CDK3ServerAdapterStartTest.class,
	CDK3ServerAdapterRestartTest.class,
	CDK3ServerAdapterConnectionTest.class
})
/**
 * @author ondrej dockal
 */
public class CDK3AllTestsSuite {

}
