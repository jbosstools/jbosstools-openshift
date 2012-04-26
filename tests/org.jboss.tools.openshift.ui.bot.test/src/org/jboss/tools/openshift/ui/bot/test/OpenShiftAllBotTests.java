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

import org.jboss.tools.ui.bot.ext.RequirementAwareSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * <b>OpenShift SWTBot TestSuite</b>
 * <br>
 * This bot test will try to demonstrate a new OpenShift Application and domain life cycle. 
 * 
 * <br>
 * TestSuite covers following test cases :
 * <ul>
 * <li>JBDS50_XXXX User credentials validation</li>
 * <li></li>
 * <li>JBDS50_XXXX Domain is created, renamed correctly</li>
 * <li>JBDS50_XXXX App with JBossAS7 cartridge is created correctly via
 * OpenShift wizards</li>
 * <li>JBDS50_XXXX Embed jenkins etc. into OpenShift applications</li>
 * <li>JBDS50_XXXX App with JBossAS7 cartridge can be deleted</li>
 * <li>JBDS50_XXXX JBoss server adapter is created successfully</li>
 * <li>JBDS50_XXXX App with JBossAS7 cartridge can be modified and republished</li>
 * <li>JBDS50_XXXX SSH keys management</li>
 * </ul>
 * 
 * @author sbunciak
 */
@SuiteClasses({

ValidateCredentials.class, 
SSHKeyManagement.class,
CreateDomain.class,
CreateAppAS7.class,
EmbeddCartrides.class,
RepublishAppASS7.class,
RenameDomain.class,
DeleteAppAS7.class, 
DestroyDomain.class

})
@RunWith(RequirementAwareSuite.class)
public class OpenShiftAllBotTests {
	/**
	 * Wrapper Suite class
	 */
}
