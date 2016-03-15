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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.jboss.tools.openshift.internal.ui.wizard.common.EnvironmentVariable;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.DeployImageWizardModel;
import org.jboss.tools.openshift.internal.ui.wizard.deployimage.PortSpecAdapter;
import org.junit.Test;
import org.mockito.Mockito;

import com.openshift.restclient.capability.resources.IImageStreamImportCapability;
import com.openshift.restclient.images.DockerImageURI;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IStatus;
import com.openshift.restclient.model.image.IImageStreamImport;

/**
 * Testing the {@link DeployImageWizardModel} class
 */
public class DeployImageWizardModelTest {

	@Test
	public void shouldInitializeContainerInfoFromLocalDockerImage() {
		// given
		final DeployImageWizardModel model = new DeployImageWizardModel();
		final IDockerConnection dockerConnection = mock(IDockerConnection.class);
		final IProject project = Mockito.mock(IProject.class);
		model.setDockerConnection(dockerConnection);
		model.setProject(project);
		// assume Docker image is on local
		final IDockerImageInfo dockerImageInfo = Mockito.mock(IDockerImageInfo.class, Mockito.RETURNS_DEEP_STUBS);
		when(dockerConnection.hasImage("jboss/wildfly", "latest")).thenReturn(true);
		when(dockerConnection.getImageInfo("jboss/wildfly:latest")).thenReturn(dockerImageInfo);
		when(dockerImageInfo.containerConfig().env()).thenReturn(Arrays.asList("foo1=bar1", "foo2=bar2"));
		when(dockerImageInfo.containerConfig().exposedPorts())
				.thenReturn(new HashSet<>(Arrays.asList("8080/tcp", "9990/tcp")));
		when(dockerImageInfo.containerConfig().volumes()).thenReturn(Collections.emptySet());

		// when
		model.setImageName("jboss/wildfly:latest");
		final boolean result = model.initializeContainerInfo();
		// then
		assertThat(result).isTrue();
		assertThat(model.getEnvironmentVariables()).contains(new EnvironmentVariable("foo1", "bar1"),
				new EnvironmentVariable("foo2", "bar2"));
		assertThat(model.getPortSpecs()).contains(new PortSpecAdapter("8080/tcp"), new PortSpecAdapter("9990/tcp"));

	}

	@Test
	public void shouldInitializeContainerInfoFromRemoteDockerImage() throws IOException {
		// given
		final DeployImageWizardModel model = new DeployImageWizardModel();
		final IDockerConnection dockerConnection = mock(IDockerConnection.class);
		model.setDockerConnection(dockerConnection);
		final IProject project = Mockito.mock(IProject.class);
		model.setProject(project);
		// no Docker image on local
		when(dockerConnection.hasImage("jboss/infinispan-server", "latest")).thenReturn(false);

		final IImageStreamImportCapability cap = Mockito.mock(IImageStreamImportCapability.class);
		when(project.supports(IImageStreamImportCapability.class)).thenReturn(true);
		when(project.getCapability(IImageStreamImportCapability.class)).thenReturn(cap);
		final IStatus status = Mockito.mock(IStatus.class);
		final IImageStreamImport streamImport = Mockito.mock(IImageStreamImport.class);
		final DockerImageURI dockerImageURI = new DockerImageURI("jboss/infinispan-server:latest");
		when(status.getStatus()).thenReturn("Success");
		when(cap.importImageMetadata(dockerImageURI)).thenReturn(streamImport);
		when(streamImport.getImageJsonFor(dockerImageURI))
				.thenReturn(getImageStreamImport("jboss_infinispan-server_ImageStreamImport.json"));
		when(streamImport.getImageStatus()).thenReturn(Arrays.asList(status));
		// when
		model.setImageName("jboss/infinispan-server:latest");
		final boolean result = model.initializeContainerInfo();
		// then
		assertThat(result).isTrue();
		assertThat(model.getEnvironmentVariables()).contains(new EnvironmentVariable("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
				new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm/java"),
				new EnvironmentVariable("INFINISPAN_SERVER_HOME", "/opt/jboss/infinispan-server"),
				new EnvironmentVariable("INFINISPAN_VERSION", "8.2.0.Final"));
		assertThat(model.getPortSpecs()).isEmpty();;
	}

	private String getImageStreamImport(final String filename) throws IOException {
		final InputStream imageStreamImport = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(filename);
		return IOUtils.toString(imageStreamImport);
	}
}
