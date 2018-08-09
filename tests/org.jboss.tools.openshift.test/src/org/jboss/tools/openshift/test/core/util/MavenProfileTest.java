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
import static org.mockito.Mockito.spy;

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
import org.junit.Test;

public class MavenProfileTest {

	private static final String SPRING_BOOT_PROJECT_NAME = "SpringBootProject";
	private static final String SPRING_BOOT_PROJECT_NAME_WITH_PROFILES = "SpringBootProjectWithProfiles";
	private static final String JAVA_PROJECT_NAME = "JavaProject";
	private static final String OPENSHIFT_MAVEN_PROFILE_ID = "openshift";

	@Test
	public void shouldNotActivateIfNoProfileId() throws CoreException, InterruptedException {
		// given
		MavenProfile profile = spy(new MavenProfile(null, 
				createMavenProject(SPRING_BOOT_PROJECT_NAME_WITH_PROFILES)));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();		
	}

	@Test
	public void shouldNotActivateIfNoProject() throws CoreException {
		// given
		MavenProfile profile = spy(new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, null));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
	}

	@Test
	public void shouldNotActivateIfProjectIsNotMavenProject() throws CoreException {
		// given
		IProject project = createJavaProject(JAVA_PROJECT_NAME);
		MavenProfile profile = spy(new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, project));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
		deleteProject(project);
	}

	@Test
	public void shouldNotActivateIfProjectHasNoProfile() throws CoreException, InterruptedException {
		// given
		IProject project = createMavenProject(SPRING_BOOT_PROJECT_NAME);
		MavenProfile profile = spy(new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, project ));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
		deleteProject(project);
	}

	@Test
	public void shouldNotActivateIfProjectHasOtherProfile() throws CoreException, InterruptedException {
		// given
		IProject project = createMavenProject(SPRING_BOOT_PROJECT_NAME_WITH_PROFILES);
		MavenProfile profile = spy(new MavenProfile("inexitantProfile", project ));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();
		deleteProject(project);
	}

	@Test
	public void shouldActivateIfProjectHasProfile() throws CoreException, InterruptedException {
		// given
		IProject project = createMavenProject(SPRING_BOOT_PROJECT_NAME_WITH_PROFILES);
		MavenProfile profile = spy(new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, project ));
		// when
		boolean activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isTrue();
		deleteProject(project);
	}

	@Test
	public void shouldNotActivateIfProfileAlreadyActive() throws CoreException, InterruptedException {
		// given
		IProject project = createMavenProject(SPRING_BOOT_PROJECT_NAME_WITH_PROFILES);
		MavenProfile profile = spy(new MavenProfile(OPENSHIFT_MAVEN_PROFILE_ID, project ));
		boolean activated = profile.activate(new NullProgressMonitor());
		assertThat(activated).isTrue();
		// when
		activated = profile.activate(new NullProgressMonitor());
		// then
		assertThat(activated).isFalse();		
		deleteProject(project);
	}

	@SuppressWarnings("restriction")
	protected IProject createMavenProject(String projectName) throws CoreException, InterruptedException {
		TestProjectProvider projectProvider = 
				new TestProjectProvider(OpenShiftTestActivator.PLUGIN_ID, null, projectName, false);
		JobUtils.waitForIdle();
		IProject project = projectProvider.getProject();
		IProjectConfigurationManager configurationManager = 
				MavenPluginActivator.getDefault().getProjectConfigurationManager();
		MavenUpdateRequest request = new MavenUpdateRequest(project, false, true);
		configurationManager.updateProjectConfiguration(request, new NullProgressMonitor());
		JobUtils.waitForIdle();
		return project;
	}

	protected IProject createJavaProject(String projectName) throws CoreException {
		IProject project = new TestProjectProvider(OpenShiftTestActivator.PLUGIN_ID, null, projectName, true).getProject();
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		JobUtils.waitForIdle();
		return project;
	}

	private void deleteProject(IProject project) {
		if (project != null) {
			try {
				project.delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				// ignore
			}
		}
	}
}
