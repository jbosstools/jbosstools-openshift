/*******************************************************************************
 * Copyright (c) 2007-2009 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.jboss.tools.openshift.ui.bot.test.explorer.CreateApp;
import org.jboss.tools.openshift.ui.bot.test.explorer.CreateDomain;
import org.jboss.tools.openshift.ui.bot.test.explorer.DeleteApp;
import org.jboss.tools.openshift.ui.bot.test.explorer.DeleteDomain;
import org.jboss.tools.openshift.ui.bot.test.explorer.EmbedCartrides;
import org.jboss.tools.openshift.ui.bot.test.explorer.RenameDomain;
import org.jboss.tools.openshift.ui.bot.test.explorer.Connection;
import org.jboss.tools.openshift.ui.bot.test.wizard.RepublishApp;
import org.jboss.tools.ui.bot.ext.RequirementAwareSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift SWTBot TestSuite</b>
 * <br>
 * This bot test will try to demonstrate a new OpenShift Application and domain life cycle. 
 * 
 * @author sbunciak
 */
@SuiteClasses({
Connection.class,
CreateDomain.class,
CreateApp.class,
EmbedCartrides.class,
RepublishApp.class,
DeleteApp.class,
RenameDomain.class,
DeleteDomain.class
})
@RunWith(RequirementAwareSuite.class)
public class OpenShiftAllBotTests {
	/**
	 * Wrapper Suite class
	 */
}
