/*******************************************************************************
 * Copyright (c) 2017-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.behavior.springboot;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.openshift.core.server.behavior.springboot.OpenShiftSpringBootProfileDetector;
import org.jboss.tools.test.util.JobUtils;
import org.jboss.tools.test.util.TestProjectProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OpenShiftSpringBootProfileDetectorTest {

	private static final String SPRING_BOOT_PROJECT_NAME = "SpringBootProject";
	private static final String CONTAINING_PLUGIN = "org.jboss.tools.openshift.test";

	private IProject project;

	@Before
	public void before() throws CoreException, InterruptedException {
		this.project = createSpringBootProject();
	}

	@Test
	public void shouldDetectSpringBootProject() {
		assertThat(new OpenShiftSpringBootProfileDetector().detect(null, null, project))
				.describedAs(
						"Wasn't able to detect the " + SPRING_BOOT_PROJECT_NAME + " project as spring boot project")
				.isTrue();
	}

	protected IProject createSpringBootProject() throws CoreException, InterruptedException {
		TestProjectProvider projectProvider = new TestProjectProvider(CONTAINING_PLUGIN, null, SPRING_BOOT_PROJECT_NAME,
				true);
		IProject project = projectProvider.getProject();
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		JobUtils.waitForIdle();
		JavaCore.create(project);
		JobUtils.waitForIdle();
		return project;
	}

	@After
	public void after() throws CoreException {
		if (project != null) {
			project.delete(true, new NullProgressMonitor());
		}
	}

}
