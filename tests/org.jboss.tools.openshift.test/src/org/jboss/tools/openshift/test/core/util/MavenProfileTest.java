/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.test.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.jboss.tools.openshift.core.util.MavenProfile;
import org.jboss.tools.openshift.internal.test.OpenShiftTestActivator;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.TestProjectProvider;
import org.junit.After;
import org.junit.Test;

public class MavenProfileTest {

	private static final String SPRING_BOOT_PROJECT_NAME = "SpringBootProject";
	private static final String JAVA_PROJECT_NAME = "JavaProject";
	private static final String OPENSHIFT_MAVEN_PROFILE_ID = "openshift";
	private static final String OTHER_MAVEN_PROFILE_ID = "other";

	private TestProjectProvider projectProvider;

	@Test
	public void shouldNotActivateIfNoProfileId() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(null, projectProvider.getProject());
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();		
	}

	@Test
	public void shouldNotActivateIfNoProject() throws CoreException {
		// given
		MavenProfile profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, null);
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
	}

	@Test
	public void shouldNotActivateIfProjectIsNotMavenProject() throws CoreException {
		// given
		this.projectProvider = createJavaProject(JAVA_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, projectProvider.getProject());
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
	}

	@Test
	public void shouldNotActivateIfProjectHasOtherProfile() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile("inexitantProfile", projectProvider.getProject());
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
	}

	@Test
	public void shouldActivateIfProjectHasProfile() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, projectProvider.getProject());
		profile.deactivate(new NullProgressMonitor());
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isTrue();
	}

	@Test
	public void shouldNotActivateIfProfileAlreadyActive() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, projectProvider.getProject());
		profile.activate(new NullProgressMonitor());
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();		
	}

	@Test
	public void shouldDeactivateIfProfileIsActive() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, projectProvider.getProject());
		profile.activate(new NullProgressMonitor());
		// when
		boolean deactivated = profile.deactivate(new NullProgressMonitor());
		// then
		assertThat(deactivated).isTrue();		
	}

	@Test
	public void shouldNotDeactivateIfProfileIsNotActive() throws CoreException, InterruptedException {
		// given
		this.projectProvider = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = new MavenProfile(OTHER_MAVEN_PROFILE_ID, projectProvider.getProject());
		profile.activate(new NullProgressMonitor());
		profile = new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, projectProvider.getProject());
		// when
		boolean deactivated = profile.deactivate(new NullProgressMonitor());
		// then
		assertThat(deactivated).isFalse();	
	}

	@SuppressWarnings("restriction")
	protected TestProjectProvider createMavenProject(String projectName) throws CoreException, InterruptedException {
		TestProjectProvider projectProvider = new TestProjectProvider(OpenShiftTestActivator.PLUGIN_ID, null, projectName, false);
		JobUtils.waitForIdle();
		IProject project = projectProvider.getProject();	
        MavenPluginActivator mavenPlugin = MavenPluginActivator.getDefault();
        IProjectConfigurationManager configurationManager = mavenPlugin.getProjectConfigurationManager();
        MavenUpdateRequest request = new MavenUpdateRequest(project, false, true);
        configurationManager.updateProjectConfiguration(request, new NullProgressMonitor());
        JobUtils.waitForIdle();
        return projectProvider;
	}

	protected TestProjectProvider createJavaProject(String projectName) throws CoreException {
		TestProjectProvider projectProvider = new TestProjectProvider(OpenShiftTestActivator.PLUGIN_ID, null, projectName, true);
		projectProvider.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		JobUtils.waitForIdle();
		return projectProvider;
	}

	@After
	public void after() {
		disposeTestProjectProvider(projectProvider);
	}

	private void disposeTestProjectProvider(TestProjectProvider projectProvider) {
		if (projectProvider != null) {
			projectProvider.dispose();
		}
	}
}
