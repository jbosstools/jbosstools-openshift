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
import org.jboss.tools.cdk.ui.bot.test.server.adapter.MinishiftServerAdapterProfilesTest;
import org.jboss.tools.cdk.ui.bot.test.server.adapter.MinishiftServerAdapterTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.MinishiftServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.MinishiftServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.download.MinishiftDownloadRuntimeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
	MinishiftServerWizardTest.class,
	MinishiftServerEditorTest.class,
	MinishiftDownloadRuntimeTest.class,
	MinishiftServerAdapterTest.class,
	MinishiftServerAdapterProfilesTest.class
})
public class MinishiftAllTestsSuite {

}
