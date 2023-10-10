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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.ServerAdapterWizardHandlingOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.HandleCustomTemplateOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ImportApplicationOS4Test;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.TemplateParametersTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.create.CreateApplicationFromTemplateOS4Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift 4 Stable Tests suite</b>
 * 
 * @contributor jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({
	TemplateParametersTest.class, // EAP  
	ImportApplicationOS4Test.class, // EAP
	CreateApplicationFromTemplateOS4Test.class, //  EAP
	ServerAdapterWizardHandlingOS4Test.class, // EAP, pass
})
public class OpenShift4SmokeTests extends AbstractBotTests {

}
