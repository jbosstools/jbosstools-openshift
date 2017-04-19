/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.server.adapter;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.core.server.adapter.ProjectBuilderTypeDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectBuilderTypeDetectorTest {

	private ProjectBuilderTypeDetector detector;
	
	@Mock
	private IProject project;
	
	@Mock
	private IFile existingFile;
	
	@Mock
	private IFile missingFile;
	
	@Before
	public void setUp() throws Exception {
		detector = new ProjectBuilderTypeDetector();
		when(existingFile.exists()).thenReturn(Boolean.TRUE);
		when(missingFile.exists()).thenReturn(Boolean.FALSE);
		when(project.isAccessible()).thenReturn(Boolean.TRUE);
		when(project.getFile(anyString())).thenReturn(missingFile);
	}

	
	@Test
	public void testRandomProject() {
		assertType(""/*unknown type*/);
	}
	
	
	@Test
	public void testMavenProject() {
		assertType("eap", "pom.xml", "package.json");
	}
	
	@Test
	public void testRubyWithGemfileProject() {
		assertType("ruby", "Gemfile", "package.json");
	}
	
	@Test
	public void testRubyWithRakefileProject() {
		assertType("ruby", "Rakefile");
	}

	@Test
	public void testRubyWithConfigRuProject() {
		assertType("ruby", "config.ru");
	}

	@Test
	public void testPhpWithIndexPhpProject() {
		assertType("php", "index.php");
	}
	
	@Test
	public void testPhpWithComposerJsonProject() {
		assertType("php", "composer.json");
	}
	
	@Test
	public void testPythonWithRequirementsTxtProject() {
		assertType("python", "requirements.txt");
	}
	
	@Test
	public void testPythonWithConfigPyProject() {
		assertType("python", "config.py");
	}

	@Test
	public void testNodeWithAppJsonProject() {
		assertType("node", "app.json");
	}
	
	@Test
	public void testNodeWithPackageJsonProject() {
		assertType("node", "package.json");
	}


	@Test
	public void testPerlWithIndexPlProject() {
		assertType("perl", "index.pl");
	}
	
	@Test
	public void testNodeWithCpanfileProject() {
		assertType("perl", "cpanfile");
	}
	
	protected void assertType(String expectedResult, String...existingFiles) {
		for (String file : existingFiles) {
			when(project.getFile(file)).thenReturn(existingFile);
		}
		String result =  detector.findTemplateFilter(project);
		assertEquals(expectedResult, result);
	}
}
