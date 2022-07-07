/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
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
import org.jboss.tools.cdk.ui.bot.test.server.adapter.CRCIntegrationTest;
import org.jboss.tools.cdk.ui.bot.test.server.editor.CRCServerEditorTest;
import org.jboss.tools.cdk.ui.bot.test.server.runtime.CRCRuntimeDetectionTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.CRCServerWizardTest;
import org.jboss.tools.cdk.ui.bot.test.server.wizard.download.CRCDownloadRuntimeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * 
 * @author odockal
 *
 */
@RunWith(RedDeerSuite.class)
@Suite.SuiteClasses({
//	CRCRuntimeDetectionTest.class,
//	CRCDownloadRuntimeTest.class,
	CRCServerWizardTest.class,
	CRCServerEditorTest.class,
	CRCIntegrationTest.class
})
public class CRCAllTestsSuite {

}
