/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.OpenNewApplicationWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.ConnectionWizardHandlingTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.CreateNewConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.OpenNewConnectionWizardTest;
import org.jboss.tools.openshift.ui.bot.test.connection.v3.RemoveConnectionTest;
import org.jboss.tools.openshift.ui.bot.test.odo.CreateDevfileComponentODOTest;
import org.jboss.tools.openshift.ui.bot.test.odo.CreateS2IComponentODOTest;
import org.jboss.tools.openshift.ui.bot.test.odo.LoginODOTest;
import org.jboss.tools.openshift.ui.bot.test.odo.ProjectManagementODOTest;
import org.jboss.tools.openshift.ui.bot.test.project.CreateNewProjectTest;
import org.jboss.tools.openshift.ui.bot.test.project.DeleteProjectTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;


/**
 * <b>OpenShift Interop Tests suite</b>
 * 
 * @contributor jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@SuiteClasses({

	LoginODOTest.class,
	ProjectManagementODOTest.class, 
	CreateS2IComponentODOTest.class,
	CreateDevfileComponentODOTest.class,
	
	OpenNewConnectionWizardTest.class, 
	CreateNewConnectionTest.class, 
	RemoveConnectionTest.class,
	ConnectionWizardHandlingTest.class, 
	
	CreateNewProjectTest.class, 
	DeleteProjectTest.class, 
	OpenNewApplicationWizardTest.class, 
})

public class OpenShiftInteropTests {
}
