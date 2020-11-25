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
package org.jboss.tools.openshift.ui.bot.test.odo;

import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftODOConnectionRequirement.CleanODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement.RequiredODOProject;
import org.jboss.tools.openshift.ui.bot.test.AbstractODOTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
/**
 * Create Component test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
@CleanODOConnection
@RequiredODOProject
public class CreateDevfileComponentODOTest extends AbstractODOTest {
	

	@InjectRequirement
	private static OpenShiftODOProjectRequirement projectReq;
	
	
	@BeforeClass
	public static void setupWorkspace() {
		importVertxLauncherProject();
	}
	
	@Test
	public void testCreateComponent() {
		createComponent(projectReq.getProjectName(), "java-vertx", true);
	}

}
