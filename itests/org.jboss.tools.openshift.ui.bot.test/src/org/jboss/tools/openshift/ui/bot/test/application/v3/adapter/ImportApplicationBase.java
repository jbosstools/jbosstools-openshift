/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.application.v3.adapter;

import static org.junit.Assert.assertNotNull;

import org.jboss.reddeer.eclipse.jdt.ui.ProjectExplorer;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement.OCBinary;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.Service;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * @author jnovak@redhat.com
 */
@OCBinary
@RequiredBasicConnection
@RequiredProject(
		name = DatastoreOS3.TEST_PROJECT)
@RequiredService(
		project = DatastoreOS3.TEST_PROJECT, 
		service = OpenShiftResources.NODEJS_SERVICE, 
		template = OpenShiftResources.NODEJS_TEMPLATE)
@RunWith(RedDeerSuite.class)
public abstract class ImportApplicationBase {
	
	@InjectRequirement
	protected static OpenShiftProjectRequirement requiredProject;
	@InjectRequirement
	protected static OpenShiftConnectionRequirement requiredConnection;
	@InjectRequirement
	protected static OpenShiftServiceRequirement requiredService;
	protected static Service service;
	protected static OpenShiftProject project;
	
	@BeforeClass
	public static void init(){
		OpenShiftExplorerView openshiftExplorer = new OpenShiftExplorerView();
		openshiftExplorer.open();
		
		OpenShift3Connection connection = openshiftExplorer.getOpenShift3Connection(requiredConnection.getConnection());
		project = connection.getProject(requiredProject.getProjectName());
		project.expand();
		service = project.getService(requiredService.getService().getName());
		
		assertNotNull("OpenShift service '" + OpenShiftResources.NODEJS_SERVICE 
					+ "' was not found!", service);
	}
	
	@Before
	public void cleanUp(){
		cleanClonnedProjects();
	}
	
	@AfterClass
	public static void cleanClonnedProjects() {
		ProjectExplorer projectExplorer = new ProjectExplorer();
		projectExplorer.deleteAllProjects();
		TestUtils.cleanupGitFolder(OpenShiftResources.NODEJS_GIT_NAME);
	}
		
}
