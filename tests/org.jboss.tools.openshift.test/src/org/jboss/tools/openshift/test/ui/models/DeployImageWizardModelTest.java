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
package org.jboss.tools.openshift.test.ui.models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizardModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IStatus;
import com.openshift.restclient.model.image.IImageStreamImport;

@RunWith(MockitoJUnitRunner.class)
public class DeployImageWizardModelTest {

	private DeployImageWizardModel model;
	private IDockerConnection dockerConnection;
	@Mock
	private IProject project;
	@Mock
	private IImageStreamImportCapability cap;
	@Mock
	private IImageStreamImport streamImport;
	@Mock
	private IStatus status;
	
	@Before
	public void setUp() {
		model = new DeployImageWizardModel();
		dockerConnection = mock(IDockerConnection.class);
		model.setDockerConnection(dockerConnection);

		model.setProject(project);
		when(project.supports(IImageStreamImportCapability.class)).thenReturn(true);
		when(project.getCapability(IImageStreamImportCapability.class)).thenReturn(cap);
		
		when(status.getStatus()).thenReturn("Success");
		when(cap.importImageMetadata(any(DockerImageURI.class))).thenReturn(streamImport);
		when(streamImport.getImageStatus()).thenReturn(Arrays.asList(status));
	}
	
	@Test
	public void testImageExistsLocally() {
		model.setProject(null);
		assertFalse(model.imageExistsLocally(null));
		assertFalse(model.imageExistsLocally(" "));
		
		//no tag
		model.imageExistsLocally("foo/bar");
		verify(dockerConnection).hasImage("foo/bar", "latest");
		
		//has tag
		model.imageExistsLocally("foo/bar:asf34fs");
		verify(dockerConnection).hasImage("foo/bar", "asf34fs");
		
		//has registry+port and tag
		model.imageExistsLocally("host:1234/foo/bar:asf34fs");
		verify(dockerConnection).hasImage("host:1234/foo/bar", "asf34fs");
		
		//
		model.setDockerConnection(null);
		assertFalse(model.imageExistsLocally("foo/bar"));
		
	}

}
