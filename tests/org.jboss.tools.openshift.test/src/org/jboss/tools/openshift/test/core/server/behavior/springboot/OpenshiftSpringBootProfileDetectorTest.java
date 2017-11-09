/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.behavior.springboot;

import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.tools.openshift.core.server.behavior.springboot.OpenShiftSpringBootApplicationProfileDetector;
import org.jboss.tools.test.util.TestProjectProvider;
import org.junit.After;
import org.junit.Test;

public class OpenshiftSpringBootProfileDetectorTest {
	
	private IProject eclipseProject;

	@Test
	public void testDetectSpringBootProject() throws Exception {
		eclipseProject = createSpringBootProject();
		assertTrue("Wasn't able to detect the SpringBoot project", new OpenShiftSpringBootApplicationProfileDetector().detect(null, null, eclipseProject));
	}

	protected IProject createSpringBootProject() throws CoreException, InterruptedException {
		TestProjectProvider projectProvider = new TestProjectProvider("org.jboss.tools.openshift.test", null, "SpringBootProject", true);
		IProject eclipseProject = projectProvider.getProject();
		eclipseProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		return eclipseProject;
	}
	
	@After
	public void tearDown() throws CoreException {
		if(eclipseProject != null) {
			eclipseProject.delete(true, new NullProgressMonitor());
		}
	}

}
