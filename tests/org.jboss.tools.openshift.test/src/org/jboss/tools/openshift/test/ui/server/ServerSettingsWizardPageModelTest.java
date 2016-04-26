/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.server.ServerSettingsWizardPageModel;
import org.jboss.tools.openshift.test.common.core.util.TestProject;
import org.jboss.tools.openshift.test.util.ResourceMocks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.openshift.restclient.model.IService;

/**
 * @author Andre Dietisheim
 */
public class ServerSettingsWizardPageModelTest {

	private ServerSettingsWizardPageModel model;
	private Connection connection;
	private IService selectedService;
	private TestProject testProject;
	private TestProject testProject2;
	private TestProject testProject3;

	@Before
	public void setUp() throws CoreException {
		this.connection = ResourceMocks.createServerSettingsWizardPageConnection();
		ConnectionsRegistrySingleton.getInstance().add(connection);
		this.testProject = new TestProject(true);
		this.testProject2 = new TestProject(true);
		this.testProject3 = new TestProject(true);
		this.model = new ServerSettingsWizardPageModel(this.selectedService = ResourceMocks.PROJECT2_SERVICES[1], null, null, connection, null) {};
		model.loadResources();
	}

	@After
	public void tearDown() {
		ConnectionsRegistrySingleton.getInstance().remove(connection);
		testProject.silentlyDispose();;
		testProject2.silentlyDispose();
		testProject3.silentlyDispose();;
	}

	@Test
	public void shouldReturn1stProjectIfNoProjecthasGitRemoteMatchingBuildConfig() {
		// given
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(testProject.getProject());
	}

	@Test
	public void shouldReturnProjectThatHasGitRemoteMatchingBuildConfig() {
		// given
		
		// when
		IProject project = model.getDeployProject();
		// then
		assertThat(project).isEqualTo(testProject.getProject());
	}
}
