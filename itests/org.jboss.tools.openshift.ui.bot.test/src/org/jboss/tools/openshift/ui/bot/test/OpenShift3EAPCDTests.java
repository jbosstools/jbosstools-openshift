/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
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
import org.jboss.tools.openshift.ui.bot.test.application.v3.adapter.PublishChangesEAPCDTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.advanced.ImportApplicationEAPCDTest;
import org.jboss.tools.openshift.ui.bot.test.application.v3.debug.DebuggingEAPAppEAPCDTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift EAP CD Tests suite</b>
 * 
 * https://issues.jboss.org/browse/JBIDE-26442
 * 
 * @author jkopriva@redhat.com
 * 
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({	
	ImportApplicationEAPCDTest.class,
	PublishChangesEAPCDTest.class,
	DebuggingEAPAppEAPCDTest.class,
	
})
public class OpenShift3EAPCDTests extends AbstractBotTests {

}
