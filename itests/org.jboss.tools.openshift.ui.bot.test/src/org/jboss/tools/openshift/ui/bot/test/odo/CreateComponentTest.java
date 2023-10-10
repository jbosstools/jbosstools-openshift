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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.junit.internal.runner.ParameterizedRequirementsRunnerFactory;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftODOConnectionRequirement.CleanODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOConnectionRequirement.RequiredODOConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement.RequiredODOProject;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
import org.jboss.tools.openshift.ui.bot.test.AbstractODOTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
/**
 * Create Component test for OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
@RunWith(RedDeerSuite.class)
@RequiredODOConnection
@UseParametersRunnerFactory(ParameterizedRequirementsRunnerFactory.class)
@CleanODOConnection
@RequiredODOProject(name="test-project25")
public class CreateComponentTest extends AbstractODOTest {

	private static final String ECLIPSE_PROJECT = "myproject";
	private OpenShiftApplicationExplorerView view;
	private OpenShiftODOConnection connection;
	
	@InjectRequirement
	private static OpenShiftODOProjectRequirement projectReq;
	
	@Parameter
	public String componentName;
	@Parameter(1)
	public String componentType;
	@Parameter(2)
	public String starter;
	
	@Parameters(name = "component type {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"mycomp", ".NET 6.0", "dotnet60-example"},
			{"mycomp", "Spring Boot", "springbootproject"},
			{"mycomp", "Vert.x Java", "vertx-http-example"},
			{"mycomp", "Python", "flask-example"},
		});
	} 

	@Before
	public void setupWorkspace() {
		view = new OpenShiftApplicationExplorerView();
		view.open();
		connection = view.getOpenShiftODOConnection();
		if (connection.getProject(projectReq.getProjectName()) == null ) {
			connection.createNewProject(projectReq.getProjectName());
		}
		importEmptyProject(ECLIPSE_PROJECT);
	}
	
	@After
	public void openshiftCleanup() {
		OpenShiftApplicationExplorerView view = new OpenShiftApplicationExplorerView();
		view.open();
		view.getOpenShiftODOConnection().getProject(projectReq.getProjectName()).getApplication(componentName).delete();
		projectReq.cleanUp();
	}
	
	@Test
	public void testCreateComponent() {
		createComponent(ECLIPSE_PROJECT, projectReq.getProjectName(), componentType, starter, componentName);
		ProjectExplorer pe = new ProjectExplorer();
		pe.open();
		assertTrue("Project " + ECLIPSE_PROJECT + "should contain devfile", pe.getProject(ECLIPSE_PROJECT).containsResource("devfile.yaml"));
		OpenShiftApplicationExplorerView view = new OpenShiftApplicationExplorerView();
		view.open();
		OpenShiftODOConnection connection = view.getOpenShiftODOConnection();
		OpenShiftODOProject project = connection.getProject(projectReq.getProjectName());
		// ToDo: getapplication returns Application object which does not exist after odo.v3
		// in test-framework part refactoring will be needed
		assertEquals(componentName, project.getApplication(componentName).getName());
	}

}
